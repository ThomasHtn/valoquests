# Refonte des défis

Plan de modification du catalogue de défis, rattaché à la refonte décrite dans `GAMEPLAY.md`.

---

## 1. Ce que contient le catalogue aujourd'hui

76 lignes en base, dont **70 actives** (V28 en a désactivé 6, V14 en avait supprimé 16 filtrées sur
Swiftplay et Escalade). Exactement 14 par difficulté.

| Difficulté | Actifs | Dont filtrés sur Compétitif |
|---|---|---|
| EASY | 14 | 8 |
| NORMAL | 14 | 10 |
| MEDIUM | 14 | 8 |
| HARD | 14 | 11 |
| VERY_HARD | 14 | 13 |
| **Total** | **70** | **50** |

**50 défis sur 70 exigent du compétitif.** Un joueur qui ne joue pas classé cette semaine-là ne peut
en valider qu'une minorité, à toutes les difficultés y compris EASY.

Trois autres constats de l'audit, qui recoupent tes règles :

- Les cibles sont des **entiers figés dans le JSON**. Elles ont été écrites pour ton escouade et pour
  personne d'autre. `VERY_HARD_COMP_KILLS` demande 600 kills : c'est un mur pour une escouade
  d'amateurs et une formalité pour des pros.
- Les défis d'agents montent en marches de comptage : 5 agents différents en EASY, 8 en MEDIUM, 10 en
  HARD, 12 en VERY_HARD, plus 12 et 25 parties avec **le même** agent. Sept lignes qui ne se
  distinguent que par un nombre.
- Les enchaînements montent de la même façon : 3 victoires d'affilée en NORMAL, 4 en HARD, 6 en
  VERY_HARD, plus 6 parties consécutives à K/D ≥ 1.
- **Aucun défi du catalogue n'est réalisable en une journée.** Le plus court, `EASY_TDM_WINS`, demande
  5 Team Deathmatch gagnés. `GAMEPLAY.md` prévoit pourtant un défi quotidien tiré « parmi les objectifs
  résolubles en une journée » : ce pool n'existe pas.

---

## 2. Tes cinq règles, traduites en décisions

### Règle 1 — Pas de compétitif hors de la difficulté la plus dure

Prise au pied de la lettre, elle supprime 37 défis et laisse EASY à 6 lignes, NORMAL à 4, MEDIUM à 6,
HARD à 3. Le catalogue ne survit pas : une difficulté qui compte moins d'entrées que la campagne n'a
de semaines se répète à l'intérieur d'une même campagne.

C'est ta règle 4 qui la sauve. Les 37 défis ne sont **pas supprimés, ils changent de filtre** :
`COMPETITIVE` devient `COMPETITIVE_OR_UNRATED`. Le défi reste identique, il devient simplement
réalisable sans jouer classé.

VERY_HARD garde `COMPETITIVE` seul. C'est sa signature : la difficulté la plus dure est la seule qui
demande du classé.

Coût assumé : un joueur qui ne joue jamais classé peut valider quatre des cinq défis hebdomadaires et
tous les défis du jour. Le cinquième lui est fermé. C'est volontaire et il faut l'afficher.

### Règle 2 — Les cibles dépendent du niveau de l'escouade

Le point important, découvert en lisant `DefaultScoringRuleset` : la **référence de calibration** ne
mesure pas le talent, elle mesure le **volume**. Les dégâts d'une partie valent 350, 425 ou 500 selon
le mode et le résultat, sans aucun rapport avec les kills réalisés. La référence est donc un nombre de
parties pondéré, rien d'autre.

Conséquence directe : la référence peut mettre à l'échelle une cible cumulée (« 180 kills dans la
semaine »), mais elle n'a **rien à dire** sur une cible par partie (« 20 kills dans une partie ») ni
sur un taux (K/D, ADR, ACS). Les mettre à l'échelle avec la référence donnerait à une escouade qui
joue beaucoup un objectif de K/D de 3,6, ce qui n'a aucun sens.

Il faut donc **deux ancrages**, tous deux calculés une fois sur les neuf mois d'historique et gelés au
lancement de la campagne :

```
facteur de volume  = référence de la campagne / 5 300
                     (5 300 = la référence de l'escouade pour laquelle le catalogue a été écrit)
                     borné à [0,4 ; 3,0]

ancres de talent   = médiane de l'escouade, par métrique et par famille de mode :
                     kills par partie Comp / Non classé, headshots, assists, score,
                     kills par Deathmatch, kills par Team Deathmatch, K/D, ADR, ACS
```

Le champ mis à l'échelle décide de l'ancrage utilisé — pas la métrique :

| Champ de la condition | Mise à l'échelle |
|---|---|
| `target` cumulé sur la fenêtre | facteur de volume |
| `target` avec `scope: PER_MATCH` | ancre de talent × coefficient de difficulté |
| `target` sur une métrique de taux (KD, ADR, ACS, HEADSHOT_RATE) | ancre de talent × coefficient de difficulté |
| `occurrences` | facteur de volume |
| `minimumMatches` | facteur de volume |
| `target` sur `PLAY_DAY`, ou `groupBy` en AGENT / GAME_MODE | **jamais** — une semaine fait 7 jours pour tout le monde |
| `streak` | plus aucune entrée n'en déclare — voir règle 3 bis |

Coefficient de difficulté appliqué aux ancres de talent :

| Difficulté | Coefficient |
|---|---|
| Quotidien | 0,85 |
| EASY | 0,90 |
| NORMAL | 1,00 |
| MEDIUM | 1,08 |
| HARD | 1,18 |
| VERY_HARD | 1,32 |

Ces coefficients reproduisent à peu près les valeurs écrites à la main aujourd'hui
(`HARD_WEEKLY_KD` 1,20 pour une médiane à 1,0 ; `VERY_HARD_COMP_ADR` 165 pour une médiane à 135), mais
sous forme d'une seule table au lieu de trente nombres épars.

Exemple sur `NORMAL_LONG_KILLS`, base 60 kills :

| Palier | Référence | Facteur | Cible |
|---|---|---|---|
| Amateur | 2 500 | 0,47 | 30 |
| Normal | 5 300 | 1,00 | 60 |
| Confirmé | 12 000 | 2,26 | 135 |
| Élite | 20 000 | 3,00 (borné) | 180 |

**Arrondi obligatoire** : au multiple de 5 sous 100, de 10 sous 1 000, de 1 000 au-delà. « Réaliser
567 kills » n'est pas un objectif lisible.

### Règle 3 — Enchaînements et agents plafonnés à 3

Le plafond est facile à poser, mais il fait s'effondrer deux échelles entières : quatre défis d'agents
et quatre d'enchaînement deviennent le même défi écrit quatre fois.

Les enchaînements ont fini par disparaître entièrement, avec les taux hebdomadaires, pour la raison
donnée plus bas. Restent les agents, plafonnés à 3 dans les deux sens, ce qui ne laisse de la place
que pour deux entrées :

- `EASY_AGENT_VARIETY` : 3 agents différents en Compétitif ou Non classé. Conservé.
- `MEDIUM_AGENT_WINS` : gagner avec 3 agents différents. Conservé, gagner est plus dur que jouer.
- `MEDIUM_AGENT_VARIETY`, `HARD_AGENT_VARIETY`, `VERY_HARD_AGENT_VARIETY` : **supprimés**, ils
  deviendraient des doublons du EASY.
- `NORMAL_MAIN_AGENT`, `VERY_HARD_MAIN_AGENT` : **supprimés**. « 3 parties avec le même agent » n'est
  un défi pour personne, et tu as raison de dire que répéter un agent n'est pas un avantage en jeu.

### Règle 3 bis — Rien qu'une seule mauvaise partie ne puisse détruire

Ajoutée après relecture du catalogue. Deux formes de défi partagent le même défaut : on les perd sans
pouvoir les rattraper.

- Les **enchaînements** (`MAX_STREAK`) : une partie ratée annule tout le travail précédent.
- Les **taux tenus sur la semaine** (`RATIO`, K/D ou ADR sur au moins N parties) : une mauvaise partie
  tire la moyenne vers le bas, et plus la semaine avance moins elle est rattrapable.

Les 6 enchaînements et les 11 taux hebdomadaires sont retirés. Les questions qu'ils posaient
survivent sous forme de `COUNT_MATCHES` : « terminer 6 parties avec un K/D d'au moins 1,20 » demande
la même chose que « tenir 1,20 sur la semaine », mais rater une partie ne coûte que cette partie.

`ProgressMode.RATIO` et `ProgressMode.MAX_STREAK` n'ont plus aucune entrée. Comme `BASELINE` depuis
`V38`, l'enum et son calculateur restent dans le code sans être déclarés par le catalogue.

### Règle 4 — Combiner compétitif et non classé

Nouveau filtre `COMPETITIVE_OR_UNRATED` = `COMPETITIVE` + `UNRATED`.

Premier en est exclu, alors que c'est le même format en 13 rounds et que le barème de dégâts le
valorise à l'identique du compétitif. La raison est le libellé : chaque défi doit nommer ses modes en
clair, et « en Compétitif, Non classé ou Premier » alourdit cent lignes pour un mode que l'escouade ne
joue pas. Il suffira d'une constante et d'un remplacement de libellé le jour où elle s'y met.

**Ne pas réutiliser `GameMode.isRoundBased()`** pour ce filtre : il est vrai aussi pour Spike Rush et
Skirmish, qui sont courts. Tu as dit « des games longues avec les mêmes contraintes » : il faut une
liste explicite.

**Le mot « partie longue » ne doit jamais atteindre l'utilisateur.** Il ne veut rien dire pour un
joueur. Chaque libellé écrit « en Compétitif ou Non classé ».

Conséquence à ne pas oublier : élargir le filtre rend les cibles cumulées mécaniquement plus faciles,
puisque plus de parties comptent. Le ré-ancrage de la règle 2 doit donc se calculer sur le **volume
long format** des neuf mois, pas sur le volume compétitif seul.

### Règle 5 — Des défis quotidiens réalisables en une ou deux parties

Un pool séparé, 21 entrées, pour tenir trois semaines sans répétition. Tirage d'un défi commun à
l'escouade chaque matin, résolution le soir même.

Trois contraintes de forme :

- Jamais de filtre `COMPETITIVE` : par construction, le quotidien n'est pas la difficulté la plus
  dure.
- Une ou deux parties maximum, tous modes confondus. Une cible qui demande trois deathmatch n'est pas
  quotidienne, c'est un mini-hebdomadaire.
- Pas de cible cumulée sur la journée quand une cible par partie dit la même chose : « une partie à
  15 kills » est lisible en jeu, « 15 kills aujourd'hui » ne l'est pas.

Les 21 entrées sont écrites dans `CHALLENGES-CATALOGUE.md`. Aucune ne demande de nouvelle métrique ni
de nouveau calculateur : ce sont les mêmes formes que l'hebdomadaire, sur une fenêtre d'un jour.

---

## 3. Ce que ça change dans le catalogue

| Action | Nombre | Détail |
|---|---|---|
| Filtre `COMPETITIVE` → `COMPETITIVE_OR_UNRATED` | 28 | tout ce qui est sous VERY_HARD |
| Cible figée → cible calculée | 70 | tout le catalogue |
| Enchaînements et taux hebdomadaires supprimés | 17 | remplacés par des `COUNT_MATCHES` |
| Défis d'agents supprimés | 5 | les échelles de comptage |
| Nouveaux défis hebdomadaires | 39 | pour atteindre 20 par difficulté |
| Nouveaux défis quotidiens | 21 | pool séparé |

Cible : **20 entrées actives par difficulté**, soit 100 hebdomadaires. Vingt permet deux campagnes de
dix semaines sans qu'une difficulté se répète. Le détail ligne par ligne est dans
`CHALLENGES-CATALOGUE.md`.

| Difficulté | Repris | À écrire | Total |
|---|---|---|---|
| EASY | 13 | 7 | 20 |
| NORMAL | 12 | 8 | 20 |
| MEDIUM | 13 | 7 | 20 |
| HARD | 11 | 9 | 20 |
| VERY_HARD | 12 | 8 | 20 |

Les groupes d'exclusion suivent le renommage : `COMPETITIVE_KILLS` devient `LONG_KILLS`, et
ainsi de suite, sinon un même pack hebdomadaire pourrait poser deux fois la même question.

---

## 4. Ce que ça demande au code

### Modèle

- `ChallengeGameMode` : ajouter `COMPETITIVE_OR_UNRATED` et `UNRATED`. La méthode `matches()` cesse d'être une
  comparaison de noms et devient un `switch`.
- Nouvelle enum `ChallengeCadence { WEEKLY, DAILY }`.
- `ChallengeCondition` : aucun changement de forme. Les cibles restent des nombres dans le JSON, ce
  sont désormais des **cibles de base** au palier Normal.

### Schéma

Deux migrations, `V40` et `V41` (V39 est la dernière appliquée) :

- `V40` : colonne `challenge.cadence` (`WEEKLY` par défaut), colonnes d'ancrage sur `run`
  (`volume_factor`, `skill_anchors_json`), colonne `weekly_challenge.resolved_conditions_json`.
- `V41` : les données — les 37 `UPDATE` de filtre, les 5 `DELETE`, les 4 réécritures, les 5 nouveaux
  hebdomadaires, les 21 quotidiens.

**Point critique.** Les cibles résolues doivent être **écrites dans `weekly_challenge` au moment du
tirage**, pas recalculées à chaque lecture. `ColonyReplayService` rejoue la campagne depuis le premier
jour à chaque synchronisation : si la cible se recalculait, un changement de roster déplacerait
rétroactivement les objectifs de semaines déjà jouées. Le tirage résout, la base stocke, tout le reste
lit.

### Services

- `ChallengeTargetResolver`, nouveau : prend une `ChallengeCondition`, le facteur de volume et les
  ancres de talent, rend la condition résolue et arrondie. C'est le seul endroit où vit la table de la
  règle 2.
- `SquadCalibrationService`, nouveau ou greffé sur l'existant : calcule le facteur de volume et les
  ancres sur les neuf mois, une fois, à l'ouverture de la campagne.
- `PlayerChallengeContextFactory` : la fenêtre devient un paramètre au lieu d'être la semaine en dur.
  Un défi quotidien se calcule avec les mêmes calculateurs sur la fenêtre du jour. **Aucun nouveau
  calculateur n'est nécessaire**, c'est ce qui rend le quotidien peu coûteux.
- `DefaultWeeklyChallengeSelectionService` : tire 5 hebdomadaires le lundi et 1 quotidien chaque
  matin, avec une fenêtre de non-répétition de 21 jours sur le pool quotidien.
- `DefaultScoringRuleset` : ajouter la valeur du défi quotidien. `GAMEPLAY.md` lui donne un poids de
  1,2 contre 1,0 pour un EASY hebdomadaire.

### Tests

`ChallengeCatalogueCompatibilityTest` charge et évalue chaque ligne du catalogue. Il faut l'étendre à
la cadence quotidienne et lui ajouter deux garde-fous qui traduisent directement tes règles :

- aucune entrée non-VERY_HARD ne déclare `COMPETITIVE` ;
- aucune entrée ne déclare `RATIO` ni `MAX_STREAK`, ni un `groupBy: AGENT` avec `target > 3`.

Exprimés sur le contenu du JSON et non sur une liste de codes, pour la raison que donne déjà `V14` :
un défi ajouté plus tard tombe sous la même règle au lieu de passer entre les mailles.

---

## 5. Les conflits que je signale

**Le quotidien pèse lourd dans le classement individuel.** Sept quotidiens à 1,2 valent 8,4 contre
1,0 pour un EASY hebdomadaire. Pour le pilier campagne c'est déjà simulé et intégré. Pour le
**classement hebdomadaire**, que la refonte ne devait pas toucher, ça déplace le curseur vers
l'assiduité au détriment de la performance. À vérifier avant de figer, pas à découvrir après.

**Élargir au Non classé rend le compétitif moins attractif.** Un joueur indifférent au classé n'a
plus aucune raison d'en jouer, sauf pour le seul défi VERY_HARD. Le composants de `GAMEPLAY.md` reste la
vraie incitation, mais l'incitation par les défis disparaît presque entièrement.

**Le facteur de volume dépend d'une calibration qui peut être faussée.** Une escouade calibrée pendant
un tournoi ou pendant les vacances traîne un facteur trop haut pendant dix semaines, sans recours,
puisque rien ne bouge en cours de campagne. La borne à 3,0 limite les dégâts sans les annuler.

---

## 6. Le plan en lots

| Lot | Contenu | Vérification |
|---|---|---|
| 1 | `ChallengeGameMode.COMPETITIVE_OR_UNRATED` et `UNRATED`, `switch` dans `matches()` | test unitaire : une partie non classée compte pour `COMPETITIVE_OR_UNRATED` et pas pour `COMPETITIVE` |
| 2 | `SquadCalibrationService` : facteur de volume et ancres sur 9 mois | test sur un historique fabriqué : facteur 1,0 pour une référence de 5 300 |
| 3 | `ChallengeTargetResolver` et sa table | test paramétré sur les six familles de champs, arrondi compris |
| 4 | `V40` schéma + résolution stockée au tirage | test d'intégration : deux recalculs successifs rendent la même cible |
| 5 | `V41` données : conversions, suppressions, réécritures, 5 nouveaux hebdomadaires | `ChallengeCatalogueCompatibilityTest` étendu, 14 actifs par difficulté |
| 6 | Fenêtre paramétrable dans `PlayerChallengeContextFactory` | les calculateurs existants passent leurs tests sur une fenêtre d'un jour |
| 7 | Pool quotidien : 21 entrées, tirage, non-répétition sur 21 jours | test : 21 tirages consécutifs sans doublon |
| 8 | Valeur du quotidien dans `DefaultScoringRuleset` + exposition API | simulation du classement hebdomadaire avant / après |

Les lots 1 à 3 sont indépendants et peuvent partir en parallèle. Le lot 5 dépend de 1 et 4. Le lot 8
est celui qui tranche le premier conflit du §5 : à faire en dernier, avec les chiffres sous les yeux.

---

## 7. Ce que je n'ai pas décidé

- **Faut-il un `SHORT_FORMAT` symétrique** (Deathmatch + Team Deathmatch) ? Tu ne l'as pas demandé et
  `MEDIUM_AIM_TRAINER` couvre déjà le cas en combinant les deux conditions. Je ne l'ai pas mis.
- **21 quotidiens**, soit trois semaines sans répétition. Tranché.
- **Le coefficient du quotidien à 1,2** vient de `GAMEPLAY.md` et n'a jamais été validé contre le
  classement individuel. C'est le premier nombre à bouger si le §5 pose problème.
