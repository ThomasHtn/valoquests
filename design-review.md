# ValoQuests — Revue UI/UX et nouvelle structure

Audit réalisé le 29/08/2026 sur l'application en fonctionnement (backend + frontend lancés,
captures à 1440 px des écrans `/overview`, `/campaign`, `/leaderboard`, `/challenges`, `/players`,
`/players/7`, `/rules`, `/tour`).

Ce document sert de base à la conception des maquettes, page par page :

- **§ 0 / 0 bis** — les acquis qui ne bougent pas, et la contrainte d'effectif variable ;
- **§ 1 / 2** — le problème de fond et le diagnostic (architecture, vocabulaire, défauts visuels) ;
- **§ 3** — la nouvelle structure, page par page, avec wireframes ;
- **§ 4** — les règles de système visuel ;
- **§ 5** — le plan d'exécution, en lots livrables séparément ;
- **§ 6** — les décisions arrêtées, toutes tranchées au 29/08 ;
- **§ 7** — seconde passe : capacités écrites mais jamais affichées, fonctionnalités manquantes,
  détails à reprendre ;
- **§ 8** — les cinq derniers arbitrages, tranchés eux aussi.

**Le document ne contient plus aucune question ouverte** : les 21 décisions du § 6 couvrent la
totalité des choix, et les maquettes peuvent partir sur cette base.

Les chiffres des wireframes reprennent les données réelles de la capture : **6 joueurs dans la
campagne, 1 hors campagne** (cf. § A8).

---

## 0. Ce qui ne bouge pas

Acquis, validés par toi, et qui contraignent tout le reste :

| Élément | Statut |
| --- | --- |
| Le layout : sidebar rétractable, `page-header`, gouttières, largeur de colonne | **Intact** |
| Le design system : tokens de `colors.css`, `clip-hex`, `notch-tr`, `label-caption`, la display face | **Intact** |
| La page profil joueur : bandeau d'identité, filtres segmentés, bandeau de KPI, historique groupé par jour, onglet Progression | **Intact**, une seule addition proposée (§ 3.5) |
| Le podium : fond gris, top 3 sur socles, reste du field en lignes | **Intact**, mais déplacé de page |
| Les tableaux à fond gris (`bg-text-primary/4` + coin biseauté + filet à gauche) | **Conservé**, resserré (§ 4.3) |

Tout ce qui suit se construit **dans** ces contraintes. Aucune proposition ci-dessous ne demande
une nouvelle typo, une nouvelle palette ou un nouveau squelette de page.

---

## 0 bis. L'effectif est une variable, pas sept

**Aucun écran ne doit supposer sept joueurs.** L'app doit tenir à 2, à 7, à 20 et au-delà.

Et l'effectif dont il est question ici, c'est **l'effectif actif** : les joueurs `INACTIVE` sont
suivis et affichés, mais ne comptent dans aucun dénominateur de campagne (présence, multiplicateur
de récolte, `n / N` sur un défi). Voir § A8 — le statut n'est pas une nuance d'affichage, c'est ce
qui définit qui est dans la partie. `N` varie donc dans le temps, y compris d'une campagne à
l'autre.

C'est déjà vrai du modèle, et depuis longtemps : `ColonyPresence.rosterSize` est une valeur figée
sur la campagne, le multiplicateur va « de ×1 seul à ×2 à `{{roster}}` », et les matériaux sont divisés
par la taille du groupe avant conversion — la page `/rules` l'écrit noir sur blanc, « ce qui rend
l'équilibre identique de 2 à 20 joueurs ». **Le domaine est agnostique ; c'est l'interface qui ne
l'est pas.**

Deux endroits où ça casse aujourd'hui :

- `ColonyView.batteryView()` construit `Array.from({ length: presence.rosterSize })` — une cellule
  par joueur, sans borne. À 20 c'est une rangée illisible, à 50 c'est une ligne de bruit.
- `ColonyView.presencePips` fait pareil, avec en plus les initiales sous chaque pastille.

> ✅ **Tranché le 29/08 : l'effectif est variable.** Le `CLAUDE.md` racine disait l'inverse
> (« follows a fixed group of seven […] not designed to scale beyond it ») — c'est ce qui m'avait
> fait dessiner sept pastilles. **Il a été corrigé** : la taille de l'escouade y est désormais
> décrite comme une variable, avec la règle « rien ne doit supposer sept » et la sémantique des
> trois `PlayerStatus`. Reste à faire côté code : borner `batteryView` et `presencePips` (lot 0).

### La règle de dessin

> **Une mesure continue porte le fait ; une liste par joueur porte le détail. C'est la liste qui
> dégrade, jamais la mesure.**

| Élément | Le fait (invariant) | Le détail (dégrade) |
| --- | --- | --- |
| Présence du jour | barre de remplissage + `3 / 20` + le multiplicateur | bande d'avatars, enveloppe sur 2 lignes, puis `+N` |
| Avancement **collectif** d'un défi | barre + `4 / 20 joueurs` | la liste des noms, au dépliement seulement |
| Classement | le tableau, qui scrolle par nature | podium top 3 + reste paginé |
| Contribution à la ville | la somme | la répartition par joueur, sur `/players` |

Corollaire pratique : **un élément par joueur ne se répète jamais plusieurs fois sur le même
écran.** Une bande d'avatars sur l'accueil, c'est bien ; la même bande sur cinq cartes de défi, ça
fait cent pastilles à 20 joueurs.

---

## 1. Le problème de fond

> L'objectif du jeu, c'est de bâtir la plus grande ville possible. **Cette ville n'existe nulle part
> dans l'interface.**

Elle est représentée aujourd'hui par un nombre — `22` — dans un hexagone, en haut à gauche de
`/overview`, dans une tuile qui occupe un quart de la rangée. À côté, sur les trois autres quarts :
un portrait de boss, son nom en 4xl, sa barre de vie. La hiérarchie visuelle raconte encore
l'ancien jeu (battre des boss), pas le nouveau (faire grandir une ville en battant des boss).

Tout le diagnostic ci-dessous découle de ça. Le reste, ce sont des conséquences et des dettes.

---

## 2. Diagnostic

### 2.1 Architecture de l'information

**A1 — La page d'accueil est un sommaire, pas une page.**
`/overview` est composée de quatre blocs qui sont chacun un résumé d'une autre page : la population
(→ `/campaign`), le boss (→ `/challenges`), la progression collective (→ `/challenges`), le podium
(→ `/leaderboard`). Elle n'a aucune question qui lui soit propre. Un visiteur qui l'ouvre voit
quatre aperçus et doit cliquer quatre fois pour lire quoi que ce soit en entier.

**A2 — La chaîne causale n'est montrée nulle part comme une chaîne.**
Le modèle est pourtant simple et linéaire :

```
matchs   ──▶ nourriture ──┐
                          ├──▶ habitants  (= le score)
défis  ──┐                │
bosses ──┴──▶ matériaux ──┴──▶ efficacité ──▶ palier de ville
```

Aujourd'hui, `/campaign` présente ces sept notions comme **neuf tuiles côte à côte**, chacune avec
sa couleur, son icône et sa bulle d'info. Un tableau de bord d'ERP. La chaîne n'est écrite qu'en
prose, sur `/rules`, trois écrans plus bas. Un coach ne la reconstruira pas.

**A3 — « Que dois-je faire aujourd'hui ? » n'a de réponse sur aucun écran.**
C'est assumé dans le code (`campaign.ts` : *« Nothing on the band says what to do tonight, on
purpose »*). Pour une escouade e-sport, c'est **la** question. Il y a une différence entre ne pas
harceler et ne pas répondre. Les données existent déjà : `presentCount`/`roster`, `surplus`,
`consumption`, `nextTierMaterials`. Une seule phrase — « 3 joueurs sur 6 ont joué aujourd'hui, le
multiplicateur passe à ×1,6 au 4ᵉ » — transforme un tableau de bord en jeu.

**A4 — La page `/challenges` n'affiche aucune progression.**
Elle liste ce que chaque défi *vaut*, jamais où on en est. La progression réelle est ailleurs, deux
fois : dans le panneau « Progression collective » de `/overview` (5 lignes, `0/6`), et dans la
matrice d'anneaux de `/leaderboard`. Un même défi est donc dessiné de **trois façons différentes**
(carte, ligne, anneau), et aucune des trois n'est sur la page qui s'appelle « Défis ».

**A5 — `/players` (« Escouade ») est déconnectée du jeu.**
Rang Valorant, winrate, K/D, HS%, matchs. Rien sur la semaine, rien sur la contribution à la ville,
rien sur la présence du jour. C'est un clone de Tracker.gg posé dans une app qui a son propre jeu.
C'est la page la plus « outil externe » de l'application.

**A6 — Un même boss est dessiné cinq fois.**
Carte héros sur `/overview`, tuile hexagonale sur la carte de `/campaign`, panneau « Boss 01 » à
droite de cette carte, carte flottante au survol de la tuile, et le tiroir `boss-detail`. Cinq
représentations, trois mises en forme des mêmes récompenses (`Récompenses` / `Échec`).
*(Toutes ne sont pas à supprimer : le tiroir est la seule vue d'historique de l'app — voir § 3.4.)*

**A7 — La moitié des données clés est derrière un survol.**
Les récompenses d'un boss, les contributions par joueur (`BossContributions`, utilisé uniquement
dans le tiroir de `/campaign`), l'en-tête de colonne de `/leaderboard` (nom + description du défi),
les explications des tuiles colonie. L'audit d'août a corrigé les bulles ⓘ ; le reste tient encore.

**A8 — Le statut d'un joueur n'est pas dessiné, et il est même dessiné à l'envers.**

`PlayerStatus` a trois valeurs, documentées sans ambiguïté dans le backend :

| Statut | Ce que ça veut dire | Visible ? |
| --- | --- | --- |
| `ACTIVE` | prend part à la compétition en entier | oui |
| `INACTIVE` | **toujours suivi et synchronisé, valide toujours ses défis individuellement, mais n'inflige jamais de dégâts au boss et n'occupe jamais de place au classement** | oui |
| `ARCHIVED` | sorti de l'effectif, historique conservé | non, absent de toute liste publique |

`INACTIVE` n'est donc **pas** « n'a pas joué ». C'est un statut d'effectif : le joueur est hors
campagne, mais il continue de jouer, de progresser et de valider ses défis dans son coin — il se
mesure aux actifs en attendant d'être intégré à la campagne suivante. Le modèle frontend le dit
aussi : *« null for an inactive player: still shown for their individual challenge progress, but
never ranked »*.

**L'interface dit le contraire.** Sur `/leaderboard`, un inactif est une ligne à `opacity-60`
étiquetée « INACTIF ». Le dégradé signifie « donnée de moindre importance » et le mot signifie
« ne joue pas ». Les deux sont faux, et la capture le prouve de façon spectaculaire :

> `MDR nataNk` est le **seul joueur de l'escouade à avoir joué** — 33 matchs, et les seuls anneaux
> de défi remplis de tout l'écran (227, 42 410, 500). Les six autres sont à zéro partout. Il est
> affiché grisé, tout en bas, sous l'étiquette « INACTIF ».

C'est le cas pathologique à concevoir, pas un cas limite : l'application a l'air morte alors que
quelqu'un joue beaucoup. J'avais moi-même mal lu cette capture et intitulé le groupe « n'ont pas
joué cette semaine » — l'exact inverse de la réalité.

**Trois conséquences pour les maquettes :**

1. **Le mot est à changer.** « Inactif » décrit ce que le joueur ne fait pas pour l'app, pas ce
   qu'il fait. Ta propre formulation est meilleure : il attend la prochaine campagne. Donc
   **« Hors campagne · rejoint la prochaine »** plutôt que « Inactif ». L'étiquette annonce une
   échéance au lieu de constater une absence. Pas de numéro : les campagnes sont identifiées par
   leurs dates (§ F1), et de toute façon un joueur retient « la suivante », pas un rang.
2. **Le traitement visuel doit dire « hors décompte », pas « moins important ».** Pas d'opacité :
   un groupe séparé, sous une règle de section, avec ses propres colonnes. Ce qui relève de la
   campagne — **position, dégâts crédités, matériaux** — vaut `—`, puisque ces valeurs n'existent
   pas pour lui. Ce qui relève de lui — **anneaux de défi, jours joués, matchs** — reste plein.
3. **Le dénominateur de la campagne, ce sont les actifs.** Présence du jour, multiplicateur de
   récolte, `n / N joueurs` sur un défi : tous comptent l'effectif **actif**. Un inactif dans ces
   dénominateurs fausserait la lecture — et c'est aussi ce qui rend `N` variable dans le temps,
   d'où le § 0 bis.

### 2.2 Vocabulaire et copy

**B1 — Trois vocabulaires pour la même chose.**

| Endroit | Mot employé |
| --- | --- |
| Sidebar | Escouade |
| Eyebrow de la page | Joueurs |
| Titre de la page | « Rang, historique et statistiques de chaque joueur » |
| Fil d'Ariane du profil | Escouade |

Idem : « Campagne » / « Run » cohabitent dans le même eyebrow (`Campagne · Run 1 · Semaine 1 sur
10`). La tuile s'appelle « Attractivité », sa description parle de « moral », `/rules` a un tableau
« moral », et les récompenses de boss affichent « Attractivité +3 ». Un lecteur croit qu'il y a
deux ressources.

**Recommandation, appliquée dans tous les wireframes de ce document : « moral ».** C'est le mot du
modèle (`Colony.morale`), celui de `/rules`, et surtout celui qui explique l'effet — un boss vaincu
remonte le moral, un boss qui tient le fait tomber. « Attractivité » nomme la conséquence
(les habitants arrivent plus vite) et laisse le lecteur deviner la cause. De même : **« campagne »**
partout, `run` restant un terme de code.

**B2 — Le lexique de la colonie est un glossaire de gestion, pas de jeu.**
Stock · Consommation · Surplus · Efficacité · Présence · Attractivité · Arrivées · Matériaux. Huit
noms abstraits sur un écran. Pour un joueur c'est froid ; pour un coach c'est un tableur. Ce sont
des mots de bilan comptable dans une app qui devrait parler de récolte, de bouches à nourrir, de
réserve, de chantier.

**B2 bis — Le tic quotidien s'appelle « la nuit ». Il doit s'appeler « la journée ».**
Le dictionnaire dit « cette nuit », « chaque nuit », « bonus sur la récolte de ce soir », et
`/rules` a une section entière intitulée **« La nuit »**. Une douzaine d'entrées FR et EN sont
concernées (`colony.delta`, `colony.hexagonAria`, `colony.track.population.description`,
`colony.track.morale.purpose`, `colony.track.arrivals.description`,
`colony.track.presence.bonus`, `rules.sections.night.*`, `rules.sections.colony.*`,
`rules.sections.town.tiersFootnote`).

C'est un mot qui vient du modèle (le rollover tourne la nuit), pas de l'expérience : le joueur, lui,
vit des **journées**. Il joue dans la journée, il regarde le lendemain ce que la journée a donné.
Parler de nuit oblige aussi à mélanger deux unités dans la même phrase — « seuls les sept derniers
**jours** comptent, chaque **nuit** la journée d'aujourd'hui entre dans le compte ».

Une seule unité partout : **la journée**. `cette nuit` → `aujourd'hui`, `chaque nuit` → `chaque
journée`, `récolte de ce soir` → `récolte du jour`, section `La nuit` → **`La journée`**.

**B2 ter — Les boss sont nommés, et le nom ne dit rien. Ils doivent être numérotés.**
« Écho déraillé » est évocateur et n'apprend rien : ni la place dans la campagne, ni la difficulté,
ni l'enjeu. `BOSS 05 · ÉLITE` apprend les deux choses actionnables — c'est le cinquième des dix
combats, et c'est un pic.

Trois raisons qui vont au-delà du goût :

1. **Ça rend le calendrier lisible.** Les catégories ne sont pas tirées au sort, elles sont
   **programmées** : `MINOR, STANDARD, STANDARD, STANDARD, ELITE, MINOR, STANDARD, STANDARD,
   STANDARD, ELITE` (`BOSS_LADDER_SHOWCASE`). Le commentaire du code le dit lui-même — c'est
   montrable justement parce que c'est un calendrier. Numérotés, `BOSS 05` et `BOSS 10` sont
   identifiables comme les deux pics de la campagne **avant** d'y arriver. Une escouade peut
   planifier. Les noms masquent exactement ça.
2. **L'app est déjà anonyme en pratique.** `imageUrl` est `null` sur toute l'entrée de catalogue :
   la carte affiche un crâne générique surmonté d'un nom. Numéroter transforme un manque en parti
   pris.
3. **La moitié du travail est faite.** `resolveBossNumberLabel()` renvoie déjà `01`, `02`, `03`
   (zéro devant, comme les plaques du podium), et `colony.boss.number` existe. `/campaign` mène
   déjà par le numéro (« BOSS 01 | MINEUR ») ; c'est `/overview` qui mène par le nom en 4xl. Ce
   n'est donc pas un changement, c'est **la fin d'une incohérence entre deux pages**.

**Le nom sort de l'interface, pas des données.** On le garde en base et au backoffice : si des
illustrations arrivent un jour, il peut revenir en sous-titre. La décision est réversible.

**B2 quater — Comment coder la difficulté : le label et la forme, pas la couleur.**
Tu proposais « un code couleur de difficulté ou un label ». Le code couleur existe déjà —
`BOSS_CATEGORY_COLORS` : `MINOR` vert, `STANDARD` bleu, `ELITE` rouge — et il n'est utilisé que sur
`/rules`. C'est précisément la collision à éviter :

- `MINOR` vert, alors que le vert veut dire « réussi / validé » partout ailleurs ;
- `ELITE` rouge, soit exactement la couleur du boss lui-même — la catégorie et le sujet portent la
  même teinte ;
- et sur la frise de campagne, chaque nœud doit déjà porter **son issue** en couleur (repoussée /
  colonie touchée / en cours / à venir). Deux codes couleur sur un même hexagone, aucun ne se lit.

D'où la répartition, qui tient à tous les effectifs et sur tous les écrans :

| Ce qui est encodé | Par quoi |
| --- | --- |
| **L'issue** du combat | la **couleur** (légende existante de la carte) |
| **La catégorie** | le **label** (`MINEUR` / `STANDARD` / `ÉLITE`) et la **forme** : hexagone plus grand, anneau plus épais pour un élite |
| **Le boss en tant que menace** | le rouge, toujours le même, jamais décliné par catégorie |

Trois catégories seulement : un mot est court, immédiat et n'a besoin d'aucune légende. « ÉLITE »
se comprend sans apprendre ce que signifie un rouge sombre.

**B3 — `/rules` annonce « Une semaine, en 6 temps » et contient 10 sections.**
Et la numérotation `01 / 02 / …` implique une séquence — or « Régularité & escouade »,
« Une difficulté qui s'ajuste toute seule », « Ce qui compte comme partie jouée » ne sont pas des
étapes d'une semaine, ce sont des barèmes de référence.

**B4 — Les textes du `/tour` sont justes mais sans identité.**
« Une aventure collective, semaine après semaine », « Chaque semaine, une menace apparaît avec des
points de vie partagés ». Aucun monde nommé, aucun enjeu, aucune voix. Le tour est par ailleurs le
**seul** endroit où la boucle complète est expliquée, il ne s'affiche qu'une fois, et on ne le
rejoue que depuis un petit lien sur `/rules`.

### 2.3 Défauts visuels concrets (relevés sur captures)

**C1 — La barre de vie du boss suit un feu tricolore inversé.**
`37 950 / 39 000 PV` s'affichent en vert quasi plein sur `/overview`. Ce n'est pas un accident :
`resolveBossHpBarColorClass()` renvoie vert au-dessus de 50 %, doré entre 20 et 50 %, rouge en
dessous — l'échelle mesure « à quel point on est près de le tuer ». L'intention se défend, le
résultat non : **le rouge y veut dire « bonne nouvelle »**, alors qu'il signifie la menace, la
défaite et la perte partout ailleurs dans l'app, et le vert y marque l'état le plus défavorable.
La même barre est rouge sur `/campaign`, sans échelle. Deux traitements, un seul fait.

→ La barre doit être **rouge, toujours**, et se vider. Le rouge est le boss ; ce qui reste de rouge
est ce qui reste à abattre. C'est lisible sans légende et ça libère le vert et le doré pour ce
qu'ils veulent dire ailleurs.

**C2 — Six accents de couleur sur le seul écran `/campaign`.**
Amber, brand-400, vert succès, cyan, rose, violet, plus le doré et le rouge de la carte. La couleur
encode la **catégorie**, jamais l'importance ni l'état. Résultat : rien ne domine, tout crie, et ça
contredit frontalement le langage « panneau gris » que tu aimes sur le podium et les tableaux.

**C3 — Les lignes du classement gaspillent leur hauteur.**
~92 px, dont une colonne d'espacement vide entre « Dégâts bonus » et la matrice d'anneaux. Les
anneaux eux-mêmes ne sont pas en cause — ils font 44 px et portent l'avancement, c'est la meilleure
cellule de l'app (§ 3.3) ; ce sont les 20 px de `py-5` et la colonne vide qui sont à récupérer.
Le vrai défaut de cette zone est ailleurs : **le nom et la cible de chaque défi n'existent que dans
une infobulle** sur l'en-tête de colonne. Et le seul joueur qui a de la vraie progression est
affiché à `opacity-60` — voir § A8, c'est un contresens à lui seul.

**C4 — La carte de campagne est vide à 80 %.**
12 rangées × jusqu'à 11 colonnes d'hexagones verrouillés pour porter 10 points de données. Elle
occupe la moitié de la page et, à côté, le panneau « Boss 01 » redit ce que la carte au survol
dirait.

**C5 — Deux hexagones identiques, deux comportements.**
L'hexagone population de `/overview` est un lien vers `/campaign` ; le même hexagone sur
`/campaign` ouvre un tiroir. Même forme, même couleur, même halo pulsant.
*(Résolu par la décision 3 : la courbe devenant permanente, l'hexagone de `/campaign` n'a plus de
tiroir à ouvrir.)*

**C6 — Les états vides ne sont pas conçus.**
Semaine 35 : `0/5` défis, six joueurs à `0` dégât, `0` matériau, un historique de campagnes rempli
de tirets. C'est pourtant l'état **normal** des premières semaines de chaque campagne. Aujourd'hui ça
ressemble à une panne.

**C7 — La 6ᵉ cellule de `/challenges` (« Prochain tirage ») est dans la grille des cinq défis.**
En pointillés, mais dans le flux. Elle se lit comme une carte manquante.

**C8 — La carte « défi » ne dit pas à qui elle s'adresse.**
Elle affiche « Dégâts 800 → Boss et classement » et « Matériaux +8 → Colonie ». C'est un tarif.
Il manque l'agent : *qui* gagne ces 800, et *quand*.

---

## 3. Nouvelle structure

### Le principe directeur

> **L'application est la chronique d'une ville qu'une escouade bâtit en jouant.**

Trois échelles de temps, plus deux vues transverses, qui deviennent la navigation :

| Échelle | Question | Page |
| --- | --- | --- |
| La journée | Que fait-on maintenant ? | **La colonie** (accueil) |
| Cette semaine | Que doit-on finir avant dimanche ? | **La semaine** |
| Dix semaines | Où en est la campagne ? | **La campagne** |
| Transverse | Qui fait quoi ? | **Escouade**, **Classement** |

### Navigation proposée

```
La colonie      (/)             ← accueil, ex-/overview
La semaine      (/week)         ← fusion de /challenges + la carte boss
Classement      (/leaderboard)  ← inchangé en route, refondu en contenu
La campagne     (/campaign)     ← recentré sur la chronique de la campagne
Escouade        (/players)      ← recentré sur la contribution
Règles          (/rules)
```

Six entrées, comme aujourd'hui. Deux changements : `/challenges` disparaît en tant que route
(redirection vers `/week`), et l'ordre suit désormais l'échelle de temps plutôt que la boucle
hebdomadaire seule.

> ✅ **Tranché le 29/08 : la fusion est retenue.** Ceci remplace l'ordre figé en août
> (`overview → challenges → leaderboard → campaign → players → rules`) et la séparation
> défis / boss. Le gain : plus aucune page n'est le résumé d'une autre. Le coût : `/challenges`
> devient une redirection permanente vers `/week`, à la manière de `/colony` → `/campaign`
> aujourd'hui.

---

### 3.1 `/` — LA COLONIE (accueil)

**Métier de la page :** montrer la ville comme un objet, et dire ce que la journée en cours lui
rapporte.

```
┌────────────────────────────────────────────────────────────────────────────┐
│ CAMPAGNE 24/08 → 01/11 · JOUR 6 / 71                  ⏳ 1j 10h            │  page-header
├────────────────────────────────────────────────────────────────────────────┤
│                                                                            │
│   ▁▂▃  ╻ ╻    ▃▂▁              ← LA VILLE (élément signature)              │
│  ▂█▄█▄─█▄█──▄███▄▂▁              silhouette qui gagne des bâtiments        │
│ ═════════════════════════════    à chaque palier, fenêtres allumées        │
│                                  = habitants / nourrissables               │
│  CAMPEMENT              22 habitants  ▲+7 aujourd'hui                      │
│  ├────────────░░░░░░░░░░░░░░░░░  675 matériaux pour atteindre HAMEAU       │
│                                                                            │
├─────────────────────────────┬──────────────────────────────────────────────┤
│ LA JOURNÉE                  │  LA SEMAINE 35            → Voir la semaine   │
│                             │                                              │
│ L'ESCOUADE AUJOURD'HUI      │  ⬢ BOSS 01 · MINEUR  ▓▓▓▓▓▓▓▓▓░ 37 950 PV   │
│ 3 / 6 ont joué       ×1,4   │  ┌──┬──┬──┬──┬──┐                            │
│ ▓▓▓▓▓▓▓▓▓▓┃░░░░░░░░░░░░░    │  │ I│II│III│IV│ V│  0 / 5 défis validés      │
│         ↑ ×1,6 au 4ᵉ        │  └──┴──┴──┴──┴──┘                            │
│ ◉ ◉ ◐ ○ ○ ○      (6 actifs) │                                              │
│                             │  CLASSEMENT      → Voir le classement        │
│ LA RÉCOLTE DE LA SEMAINE    │  1 kikoucraft 1 650 · 2 Psilonnix 0 · 3 …    │
│ ▓▓▓▓▓▓▓▓▓▓┃░░░░░░  14       │                                              │
│ 3 mangés · 11 de surplus    │                                              │
│ ─────────────────────────── │                                              │
│ Il manque 1 joueur          │      (à 20 joueurs : la bande d'avatars      │
│ pour passer ×1,6.           │       enveloppe sur 2 lignes puis « +N » ;   │
│                             │       tout le reste est inchangé)            │
└─────────────────────────────┴──────────────────────────────────────────────┘
```

**Bloc 1 — La ville (signature).**
Une silhouette dessinée en SVG/CSS, dans les tons `surface` avec les fenêtres en `brand-500`. Elle
n'est pas décorative, chaque élément encode une donnée :
- nombre et hauteur des bâtiments = **palier** (Campement → Hameau → Village → … → Citadelle) ;
- fenêtres allumées = **population / nourrissables** ;
- échafaudage sur le prochain bâtiment = **progression en matériaux** vers le palier suivant.

C'est le seul endroit où l'app prend un risque visuel, et c'est justifié : c'est l'objectif du jeu,
rendu lisible sans une phrase d'explication. Un coach comprend « la ville grandit » en une seconde.

**Coût : plus faible que prévu — le modèle porte déjà les silhouettes.** `colony-tier.utils.ts`
groupe les douze paliers en **quatre bandes** — `CAMP`, `HOUSES`, `SKYLINE`, `MONUMENT` — et
`ColonyView.ladder` produit déjà l'état de chaque palier. Il faut donc dessiner **quatre
silhouettes**, pas une par palier, et la densité à l'intérieur d'une bande porte la progression
fine. Voir § D1 : ce bloc est à moitié écrit.

**Bloc 2 — La journée.** Le seul bloc prescriptif de l'application. Le titre nomme l'unité de temps
du jeu : une journée de jeu se solde et se lit le lendemain, c'est le battement de base de la
colonie.

Deux visualisations et **une** phrase. Chacune suit la règle du § 0 bis — la mesure est invariante,
seule la bande d'avatars dégrade :

1. **L'escouade aujourd'hui — une barre à seuil, puis les visages.**
   *Le fait, invariant :* une barre de remplissage continue (`3 / 6`, ou `3 / 20`, ou `3 / 50` —
   toujours sur l'effectif **actif**, § A8),
   le multiplicateur atteint en chiffre, et **un marqueur là où tombe le palier suivant**. Une barre
   continue avec un marqueur ne change pas de forme avec l'effectif — c'est ce qui remplace la
   segmentation par joueur que j'avais proposée, et qui aurait donné 20 segments à 20 joueurs.
   *Le détail, qui dégrade :* sous la barre, une bande d'avatars dans les trois états que le modèle
   connaît déjà (`FULL` a joué / `PARTIAL` sous le seuil / `NONE` n'a pas joué). Elle enveloppe sur
   deux lignes au maximum, puis tronque avec `+N`. À 7 elle tient sur une ligne et c'est agréable ;
   à 20 elle en prend deux ; au-delà elle se coupe sans que la mesure au-dessus ne bouge.
   → C'est la bande qui rend le bloc actionnable : la barre dit « 3 sur 6 », seuls les visages
   disent **qui manque**. C'est aussi ce qui lui donne son allure de jeu.
2. **La récolte de la semaine — une barre avec un marqueur.** La barre est la récolte, le marqueur
   est la consommation, la partie à droite du marqueur est le surplus. La question réelle est un
   seuil (« la récolte couvre-t-elle la consommation ? »), et un seuil se lit sur une barre, pas
   dans trois nombres à additionner de tête. Invariante par nature : elle ne dépend pas de
   l'effectif.
3. **Une seule phrase, sous un filet :** la conclusion. « Il manque 1 joueur pour passer ×1,6. »
   Aucun graphique ne dit ça ; c'est le seul endroit du bloc où le texte est irremplaçable. Elle est
   invariante aussi — elle nomme un écart, pas une liste.

> **Coût réel : quasi nul.** `ColonyView.presencePips` existe déjà — un objet par joueur avec
> `playerId`, `name`, `initials`, `state`, `fillClass`, `ariaLabel` — et **n'est rendu dans aucun
> template** (`/campaign` affiche à la place le `battery()` anonyme, « 0/6 joueurs actifs
> aujourd'hui »). Le view model est écrit et dort dans le code ; il ne lui manque qu'une borne
> d'affichage (cf. § 0 bis).

**Bloc 3 — La semaine, en résumé.** Boss (barre rouge) + cinq créneaux de défis + compteur. C'est
un **lien** vers `/week`, pas une copie : trois chiffres et cinq états, rien de plus.

**Bloc 4 — Classement en trois lignes.** Le podium quitte cette page (voir § 3.3).

**Ce qui disparaît :** la carte boss héros, la tuile population isolée, le panneau « Progression
collective » avec son `0 / 5` en 6xl, le podium.

---

### 3.2 `/week` — LA SEMAINE

**Métier de la page :** ce que l'escouade doit finir avant dimanche 23h59.

```
┌────────────────────────────────────────────────────────────────────────────┐
│ SEMAINE 35 · 24/08 – 30/08                            ⏳ 1j 10h            │
├────────────────────────────────────────────────────────────────────────────┤
│ ⬢  BOSS 01        MINEUR                          1 / 10 de la campagne    │
│ ████████████████████████████████████████████░░░  37 950 / 39 000 PV        │
│ Vaincu → +240 matériaux, +3 moral   ·   Survivant → −7 moral               │
├────────────────────────────────────────────────────────────────────────────┤
│ LES CINQ DÉFIS                                        12 100 dégâts en jeu │
│                                                                            │
│ ┌────────────────────────────────┐ ┌────────────────────────────────┐      │
│ │ ⬡I  FACILE                     │ │ ⬡II NORMAL                     │      │
│ │ CLIC PRÉCIS                    │ │ BOURREAU                       │      │
│ │ 120 headshots en Deathmatch    │ │ 250 kills en Compétitif        │      │
│ │ ░░░░░░░░░░░░░  0 / 6 joueurs   │ │ ▓▓░░░░░░░░░░░  1 / 6 joueurs   │      │
│ │ 800 dégâts · +8 matériaux      │ │ 1 400 dégâts · +14 matériaux   │      │
│ └────────────────────────────────┘ └────────────────────────────────┘      │
│                        …trois autres cartes…                               │
├────────────────────────────────────────────────────────────────────────────┤
│ SI LA SEMAINE EST PARFAITE : +365 matériaux → la ville passe HAMEAU        │
├────────────────────────────────────────────────────────────────────────────┤
│ PROCHAIN TIRAGE · LUNDI 31/08                                              │
│ Cinq nouveaux défis parmi les 63 du catalogue.                             │
│   ↑ la date existe déjà ; l'aperçu du catalogue viendra ici (§ 8.5 / F4)   │
└────────────────────────────────────────────────────────────────────────────┘
```

Changements par rapport à `/challenges` :
1. **Le boss remonte ici**, en bandeau compact — plus de portrait héros, il n'est plus le sujet du
   jeu. Barre **rouge** qui se vide (jamais verte). Gains et pertes sur une ligne.
2. **Chaque carte de défi gagne la progression d'escouade** : une barre de remplissage + `n / N
   joueurs`. C'est la donnée qui manquait, elle existe déjà côté API.
   **Pas de pastille par joueur ici**, contrairement à l'accueil : cinq cartes × 20 joueurs feraient
   cent pastilles sur un écran (§ 0 bis). Une bande d'avatars ne se justifie qu'une fois par écran,
   et c'est sur l'accueil qu'elle sert. Le « qui » vient au dépliement de la carte.

   > **Deux faits distincts, deux endroits.** `n / N joueurs` est l'avancement **collectif** —
   > combien ont fini. L'avancement **individuel** — où chacun en est dans son accumulation — est
   > porté par les anneaux, au classement et sur le profil (§ 3.3, § 3.5). Cette carte ne remplace
   > pas les anneaux et ne les répète pas : elle répond à une autre question.
3. **Le pied de page relie la semaine à l'objectif** : ce que la semaine parfaite rapporterait, et
   ce que ça déclencherait sur la ville.
4. **« Prochain tirage » sort de la grille des cinq** et devient une bande sous les cartes — c'est
   aussi l'emplacement réservé à l'aperçu du catalogue (§ 8.5). Les deux disent la même chose à deux
   niveaux de détail : quand tombe le prochain tirage, et parmi quoi. La date existe aujourd'hui,
   l'aperçu viendra avec l'endpoint.

Les cartes elles-mêmes sont bonnes (tiers colorés, hexagone de difficulté, description). On garde.

---

### 3.3 `/leaderboard` — CLASSEMENT

**Métier :** qui a le plus contribué, cette semaine et les précédentes.

Le **podium arrive ici**, en tête de page : c'est sa place naturelle, et ça donne à la page de
classement un héros qu'elle n'a pas aujourd'hui. Sous lui, le tableau resserré.

```
┌────────────────────────────────────────────────────────────────────────────┐
│ ‹  SEMAINE 35   24/08 – 30/08   ● EN COURS                              ›  │
├────────────────────────────────────────────────────────────────────────────┤
│                          ┌────────┐                                        │
│              ┌────────┐  │   01   │  ┌────────┐        ← podium, inchangé  │
│              │   02   │  │  1650  │  │   03   │                            │
├────────────────────────────────────────────────────────────────────────────┤
│              ⬡I      ⬡II     ⬡III    ⬡IV     ⬡V     ← nom + cible visibles │
│              120     250      10    80 000  1 200      (plus au survol)    │
│                                                                            │
│  JOUEUR          DÉGÂTS  BONUS  MATÉR.  JOURS │  ◔     ◕     ○     ◑    ◯  │
│ #1 ◉ kikoucraft   1 650   +600    +22     3   │ 40    180    2   12 400  0 │
│ #2 ◉ Psilonnix        0     —       0     0   │  0      0    0       0   0 │
│ …                                        (6 joueurs dans la campagne)      │
├────────────────────────────────────────────────────────────────────────────┤
│ HORS CAMPAGNE · REJOIGNENT LA PROCHAINE                                   │
│ Ils jouent et valident leurs défis, sans compter au classement ni à la ville│
│                                                                            │
│ ◉ MDR nataNk        —      —       —     3    │  0    227    0   42 410 500│
└────────────────────────────────────────────────────────────────────────────┘
```

Changements :
- **Les anneaux restent.** Voir ci-dessous — c'est la meilleure cellule de l'application.
- **Hauteur de ligne 92 → ~72 px** : c'est l'anneau (44 px) qui fixe le plancher, on récupère les
  20 px de `py-5` et la colonne d'espacement vide. Pas 64 px : je m'étais fixé une cible qui
  impliquait de supprimer l'anneau.
- **Nouvelle colonne « Matériaux »** : la contribution à la ville. C'est le lien entre les deux
  piliers, absent partout aujourd'hui.
- **Le nom et la cible du défi sortent du survol** et s'écrivent dans l'en-tête de colonne, sous le
  badge de difficulté. C'était ça, le vrai problème de cette zone — pas l'anneau.
- **Les hors-campagne sortent du dégradé d'opacité** et forment un groupe nommé, sous sa propre
  règle de section, avec une phrase qui dit ce que le groupe est (§ A8). Ce n'est pas une donnée
  dégradée, c'est une donnée d'une autre nature : **dégâts, position et matériaux valent `—`**
  (ils n'existent pas pour ces joueurs), **les anneaux de défi et les jours joués restent pleins**.
  C'est exactement ce qui leur permet de se mesurer aux actifs en attendant la campagne suivante.
- **Sur une semaine close, un lien vers son nœud de campagne.** Le pas de semaine existe déjà et
  lit `RankingApi.history` ; il ne manque que le retour vers `/campaign`. Le tiroir de boss mène
  déjà ici (`?week=`), la réciproque manque — c'est la même semaine lue par ses deux piliers
  (§ 3.4).

#### Pourquoi les anneaux restent

> **Correction.** J'avais proposé de les remplacer par des pastilles binaires. C'était une erreur,
> et une erreur de méthode : je les ai jugés sur une capture de la semaine 35 où **toutes les
> valeurs étaient à zéro**. Cinq anneaux vides sur sept lignes ressemblaient à du bruit. La seule
> ligne portant de vraies données — `MDR nataNk`, 227 / 42 410 / 500 — montrait au contraire les
> anneaux faisant exactement leur travail, à des taux de remplissage différents. J'ai condamné un
> composant sur un jeu de données vide.

Un défi ne se valide pas d'un coup : « jouer 40 deathmatchs », « infliger 80 000 dégâts » se
construisent sur plusieurs jours. **L'information utile est donc l'avancement, pas l'état
terminé.** Une pastille binaire répond « non » pendant six jours puis « oui » — elle est aveugle
exactement pendant la durée où on la consulte.

L'anneau tel qu'il est aujourd'hui porte trois choses dans 44 px : l'arc (le pourcentage), la
valeur courante au centre, la couleur de difficulté. Le modèle est propre —
`completionPercentage`, `currentValueLabel`, `targetValueLabel`. C'est un cas d'école du § 4.0 :
dénominateur réel (valeur / cible), mesure continue, et un **nombre de colonnes fixe** — cinq défis
par semaine, quel que soit l'effectif, donc rien à dégrader au sens du § 0 bis.

**Conséquence que j'avais ratée : c'est le seul endroit de l'application où l'avancement individuel
d'un défi existe.** Un joueur qui veut savoir où il en est doit chercher sa ligne dans un tableau
de classement. C'est un manque, pas un doublon — d'où l'ajout au bandeau « cette semaine » du
profil (§ 3.5).

---

### 3.4 `/campaign` — LA CAMPAGNE

**Métier :** la chronique de la campagne sur dix semaines. Ce n'est plus un tableau de bord temps réel
(ça, c'est l'accueil), c'est une **histoire**.

```
┌────────────────────────────────────────────────────────────────────────────┐
│ CAMPAGNE 24/08 → 01/11 · SEMAINE 4 SUR 10   ⚔ 2/10 vaincus  JOUR 25 / 71   │
├────────────────────────────────────────────────────────────────────────────┤
│ LA CAMPAGNE                                                                │
│  ⌬ ⌬ ⌬ ⌬ ⌬ ⌬ ⌬ ⌬ ⌬ ⌬ ⌬ ⌬ ⌬ ⌬ ⌬ ⌬ ⌬ ⌬ ⌬ ⌬ ⌬ ⌬ ⌬  ← nappe décorative, très basse│
│ ⌬ ⌬ ⌬ ⌬ ⌬ ⌬ ⌬ ⌬ ⌬ ⌬ ⌬ ⌬ ⌬ ⌬ ⌬ ⌬ ⌬ ⌬ ⌬ ⌬ ⌬ ⌬ ⌬     aria-hidden, non cliquable  │
│  01     02    03    04    05     06    07    08    09    10                │
│  ⬢──────⬡─────⬡─────⬡─────⬢──────⬡─────⬡─────⬡─────⬡─────⬢                 │
│ MINEUR  STD   STD   STD  ÉLITE  MINEUR STD   STD   STD  ÉLITE              │
│  ✔      ✔      ✘     ●                                                     │
│ ······ repoussée / touchée ······│ en cours │····· à venir ·············    │
│ ⌬ ⌬ ⌬ ⌬ ⌬ ⌬ ⌬ ⌬ ⌬ ⌬ ⌬ ⌬ ⌬ ⌬ ⌬ ⌬ ⌬ ⌬ ⌬ ⌬ ⌬ ⌬ ⌬                                │
│         ↑ le numéro EST la semaine ; les gros hexagones sont les élites    │
│         ↑ seuls les dix nœuds sont cliquables → tiroir de la semaine       │
│                                                                            │
│              ┌─ tiroir, ouvert sur le nœud 03 ─────────┐                   │
│              │ ‹  BOSS 03 · STANDARD   S3 · 07–13/09 › │  ← pas de semaine │
│              │ ✘ Colonie touchée   4 200 / 46 000 PV   │                   │
│              │ Coût : −7 moral · 0 matériau            │                   │
│              │ ─────────────────────────────────────── │                   │
│              │ LE CLASSEMENT DE CETTE SEMAINE          │                   │
│              │ #1 ◉ kikoucraft              12 400     │  ← contributions  │
│              │ #2 ◉ getjfox                  9 100     │                   │
│              │ …                                       │                   │
│              │ → Le classement complet de la S3        │                   │
│              └─────────────────────────────────────────┘                   │
│                                                                            │
│ POPULATION DE LA CAMPAGNE                                                     │
│  50┤                                          ╭───                         │
│    │                              ╭───────────╯      ← courbe permanente   │
│   0┼──────────────────────────────╯                    (plus dans un tiroir)│
│    J1                                              J71                     │
├────────────────────────────────────────────────────────────────────────────┤
│ ▸ LE DÉTAIL DE L'ÉCONOMIE            (replié par défaut, à toutes largeurs) │
├────────────────────────────────────────────────────────────────────────────┤
│ LES CAMPAGNES                    durée      score                          │
│ 24/08 → 01/11  (en cours)      sem. 4/10      48 →                         │
│ Première campagne — le score s'inscrira au jour 71                         │
│   ↑ en cours : la semaine atteinte, pas une durée ; le score est provisoire│
│                                                                            │
│ (identité = la plage de dates, jamais un numéro — § F1. Une campagne       │
│  arrêtée s'inscrit « interrompue · 6 sem. » : la durée est affichée sans   │
│  quoi les scores se compareraient à tort)                                  │
└────────────────────────────────────────────────────────────────────────────┘
```

Changements :
1. **La carte hexagonale devient une frise horizontale de dix nœuds, sur une nappe d'hexagones
   décorative.** ✅ **Tranché le 29/08**, dans cette variante précisément. Un nœud par semaine : le
   boss, son issue, les matériaux banqués, la population en fin de semaine. On passe de ~150
   hexagones portant 10 données à 10 nœuds portant 40 données.

   **La nappe reste, mais change de statut : elle devient décor, plus structure.** C'est ce qui
   sauve l'ambiance sans payer son coût. Trois règles pour qu'elle reste du décor :
   - `aria-hidden`, non cliquable, non focusable — plus aucun hexagone de terrain n'est un contrôle ;
   - un seul ton, très bas contraste (`text-primary/4` à `/8`), sans halo ni animation : c'est un
     fond, il ne doit jamais concurrencer les dix nœuds posés dessus ;
   - elle ne porte plus la géométrie. Les nœuds sont placés par la frise, pas par le serpentin —
     donc plus de `resolveBossColumn`, plus de rangées de terrain de tête et de queue, et la nappe
     peut être une simple répétition CSS qui déborde le cadre.

   **La numérotation des boss est ce qui rend la frise lisible** (§ B2 ter) : le nœud `05` porte le
   numéro du boss *et* la semaine de campagne, puisque c'est le même nombre. Les deux élites
   programmés, `05` et `10`, se repèrent d'un coup d'œil à leur hexagone plus grand — la frise
   devient un calendrier que l'escouade peut anticiper, ce qu'une suite de noms propres ne permet
   pas.
2. **La courbe de population devient permanente**, sous la frise. C'est le récit de la campagne.
   ✅ **Tranché le 29/08.** Ceci revient sur une décision du 28/08 (courbe dans un tiroir +
   sparkline dans la tuile) ; l'argument qui a changé est que la page n'est plus un tableau de bord
   de l'instant, donc son second bloc doit être l'historique et non un raccourci vers lui.
   Conséquences : la sparkline de la tuile population disparaît (la vraie courbe est juste en
   dessous), et le tiroir de courbe est retiré — l'hexagone population n'a donc plus de tiroir à
   ouvrir, ce qui règle au passage le § C5 (deux hexagones identiques, deux comportements).
3. **Les huit tuiles économiques deviennent une section « détail », repliée par défaut à toutes les
   largeurs** (aujourd'hui : repliée sous `lg` uniquement). Ce sont des définitions de règles, pas
   un tableau de bord ; leur place est en référence.
4. **Le panneau latéral « Boss 01 » disparaît — le tiroir, lui, reste.** Ce sont deux choses
   différentes que j'avais confondues : le panneau était un doublon permanent de la semaine en
   cours, le tiroir est la vue de détail de *n'importe quelle* semaine. Voir ci-dessous.

#### Le tiroir de boss reste — c'est lui, l'historique de la campagne

> **Manque dans ma première version.** J'ai écrit ce qui disparaissait sans dire ce que devenait le
> tiroir ouvert au clic sur une tuile. Or c'est le seul endroit de l'application où l'on peut
> consulter une semaine passée en entier.

Ce qu'il porte aujourd'hui, et qui doit être conservé tel quel :

| Contenu | Pourquoi c'est irremplaçable |
| --- | --- |
| Le boss de la semaine, son issue, ses PV | La frise donne l'issue ; le tiroir donne le détail du combat |
| Récompenses (matériaux, efficacité, moral) et coût de l'échec | Ce que cette semaine-là a réellement rapporté à la ville |
| **`app-boss-contributions`** — les dégâts de la semaine par joueur, classés | **Le classement de cette semaine-là.** Ce composant n'est utilisé nulle part ailleurs dans l'app |
| Navigation semaine précédente / suivante | On parcourt la campagne combat par combat sans repasser par la carte |
| Lien `/leaderboard?week=…` | Le classement complet de la semaine, déjà câblé |

**La frise améliore ce tiroir au lieu de le menacer.** Aujourd'hui les dix tuiles sont dispersées
sur un serpentin au milieu de 150 hexagones de terrain : la navigation précédent/suivant du tiroir
n'a aucun équivalent spatial, on ne voit pas où l'on est. Sur une frise, les dix nœuds sont alignés
et adjacents — le pas du tiroir devient un déplacement visible d'un nœud au suivant. Et avec les
boss numérotés (§ B2 ter), le titre du tiroir (`BOSS 05 · ÉLITE`) est littéralement l'étiquette du
nœud d'où il a été ouvert.

**Deux chemins vers une semaine passée, à relier dans les deux sens.** Le tiroir mène déjà au
classement de la semaine (`?week=`) ; le pas de semaine du classement ne mène nulle part vers la
campagne. Ajouter le retour : depuis une semaine close du classement, un lien vers son nœud de
frise. C'est la même semaine lue par ses deux piliers.

---

### 3.5 `/players` — ESCOUADE

**Métier :** qui compose l'escouade et ce que chacun apporte à la ville.

Un seul mot partout : **Escouade** (sidebar, eyebrow, titre, fil d'Ariane).

**Inversion des colonnes** — le jeu d'abord, Valorant ensuite :

```
 DANS LA CAMPAGNE EN COURS · 6 joueurs
   JOUEUR          CETTE SEMAINE                    │  VALORANT
                   dégâts  jours  défis  matériaux  │  rang     winrate  K/D
◉ kikoucraft  ●     1 650    3     2/5     +22      │  Platine 2  0 %    0.00
◉ Psilonnix   ○         0    0     0/5       0      │  Diamant 1  0 %    0.00
 …

 HORS CAMPAGNE · REJOIGNENT LA PROCHAINE · 1 joueur
◉ MDR nataNk  ●         —    3     2/5       —      │  Radiant   42 %    1.50
```

- La pastille `●` avant le nom = **a joué aujourd'hui**. Le multiplicateur de récolte en dépend et
  aucun écran ne le montre joueur par joueur.
- Le bloc Valorant reste, séparé par un filet vertical : c'est un contexte, plus le sujet.
- Traitement de ligne conservé (fond gris, coin biseauté, filet à gauche).

**Profil joueur — une seule addition.** Le bandeau d'identité a de la place à droite, il ne porte
que le rang Valorant. Y ajouter la ligne du jeu :

```
┌──────────────────────────────────────────────────────────────────────┐
│ 👤  KIKOUCRAFT           CETTE SEMAINE                │  Platine 2 ⬢ │
│     #EUW         1 650 dmg · 3 jours · +22 mat · #1   │   50 RR      │
│                  ◔    ◕    ○    ◑    ◯    ← ses 5 défis, mêmes       │
│                  40  180   2  12 400  0      anneaux qu'au classement│
└──────────────────────────────────────────────────────────────────────┘
```

Les cinq anneaux repris ici, à l'identique : c'est la réponse à « où j'en suis », qui n'existe
aujourd'hui que dans une ligne du tableau de classement (§ 3.3). Même composant, même lecture, une
seule implémentation.

**Deux variantes du bandeau, selon le statut** (§ A8). Pour un joueur hors campagne, dégâts crédités,
matériaux et **position au classement** n'existent pas — les afficher à `0` mentirait sur son
investissement, alors que c'est précisément le joueur qui a le plus joué dans la capture. Son rang
Valorant, lui, reste : il ne dépend pas de la campagne.

```
┌──────────────────────────────────────────────────────────────────────┐
│ 👤  MDR NATANK    HORS CAMPAGNE · rejoint la prochaine   │ Radiant ⬢ │
│     #1wnl         3 jours joués · 33 matchs              │ 519 RR    │
│                   ○    ◕    ○    ◑    ◔   ← ses défis comptent      │
│                   0   227   0  42 410 500    pour lui, pas pour la  │
│                                              ville                  │
└──────────────────────────────────────────────────────────────────────┘
```

C'est la page qui porte la promesse du statut : **il progresse, il se compare, il n'est simplement
pas décompté.** Tout le reste du profil — filtres, KPI, historique des matchs, progression — est
identique pour les deux statuts, et c'est bien ce qui doit rester : le hors-campagne a exactement
les mêmes outils de mesure que les autres.

Tout le reste de la page (filtres segmentés, bandeau de KPI, historique groupé par jour avec sa
ligne d'agrégat, onglet Progression) est **intact**. C'est le meilleur écran de l'app et c'est le
patron dont les autres pages devraient s'inspirer : *identité → portée → chiffres clés → liste
groupée*.

---

### 3.6 `/rules`

Deux parties au lieu d'une liste de dix sections numérotées :

**Partie 1 — Comment marche une semaine** (numérotée, parce que c'est vraiment une séquence) :
`01` lundi, cinq défis sont tirés · `02` vous jouez, chaque match tape · `03` dimanche 23h59, le
boss tombe ou survit · `04` chaque journée, la ville grandit.

**Partie 2 — Les barèmes** (non numérotée, c'est de la référence) : dégâts par mode, bonus de
régularité et d'escouade, rendement du jour, catégories de boss, moral, paliers de ville.

En tête de page, **le schéma de la chaîne** (§ 2.1 A2) dessiné une fois. Il remplace trois
paragraphes et sert aussi au `/tour`.

Corriger « Une semaine, en 6 temps » (il y a dix sections aujourd'hui).

---

### 3.7 `/landing` et `/tour`

La structure est bonne, elle ne change pas. **C'est la copy qu'il faut réécrire.**

Ce qui manque aujourd'hui : un monde nommé, un enjeu, une voix. Trois principes pour la réécriture :

1. **Nommer la chose.** Pas « une colonie », pas « une menace ». La ville a un nom de palier
   (Campement, Hameau, Village…) : s'en servir. « Vous avez dix semaines et un campement. »
2. **Chiffrer.** « Chaque partie nourrit la colonie » ne dit rien. « Une victoire en compétitif :
   500 de nourriture. Une semaine où toute l'escouade joue : de quoi passer un palier. » Les
   chiffres sont dans `rules.constants.ts`, ils sont bons, ils sont concrets, ils font le travail.
   Attention aux formulations qui figent l'effectif (« vous êtes sept ») : il varie (§ 0 bis).
3. **Dire l'enjeu à la fin.** Le score final, c'est la population au jour 71. Le tour ne le dit
   qu'en passant, dans la 2ᵉ moitié d'un paragraphe.

Ajouts fonctionnels :
- Le tour devient rejouable depuis le pied de la sidebar, pas seulement depuis `/rules`.
- La 6ᵉ étape (« À découvrir ») devient **« Ce qu'on attend de vous »** : trois verbes, jouer /
  valider / tenir la semaine. On finit sur une action, pas sur une invitation à flâner.

---

## 4. Système visuel — cinq règles d'emploi

Aucune ne touche aux tokens. Ce sont des règles d'emploi.

### 4.0 Une jauge exige un dénominateur réel

C'est la règle qui manquait, et c'est elle qui explique pourquoi `/campaign` fatigue alors que le
bloc « La journée » de l'accueil doit au contraire être graphique. Le critère n'est pas
« graphique ou texte », c'est **ce que la donnée est** :

| La donnée est… | Traitement | Exemples |
| --- | --- | --- |
| un **compte sur un ensemble de taille variable** | barre + `n / N` ; les visages en second, bornés (§ 0 bis) | présence du jour, défi validé par `n` joueurs |
| un **palier sur une échelle bornée** | segments cisaillés (`clip-shear`), **si le nombre de segments est fixe** | avancement des 5 défis de la semaine, étapes du `/tour` |
| une **part d'un tout réel** | barre, éventuellement avec marqueur de seuil | récolte vs consommation, PV du boss |
| une **accumulation en cours**, qui met des jours à se remplir | **anneau ou barre, jamais un état binaire** | avancement d'un défi (40 deathmatchs, 80 000 dégâts) |
| une **série dans le temps** | courbe | population de la campagne |
| une **définition de règle** | **texte seul** | « Efficacité : habitants nourris par nourriture » |
| une **conclusion** | **une phrase** | « Il manque 1 joueur pour passer ×1,6 » |

Les deux dernières lignes sont ce que `/campaign` viole : ses dix tuiles sont des *définitions*, et
chacune porte pourtant une icône, un filet coloré et une jauge. Une définition n'a pas de
dénominateur, donc sa jauge est forcément fausse ou empruntée — le cas est déjà documenté dans le
code : `Colony.efficiency` n'a **pas de plafond**, donc son anneau est rempli avec
`tierProgressPercentage`, un pourcentage qui appartient à un autre bloc de la même page.

À l'inverse, présence (dénominateur : l'effectif), multiplicateur (échelle ×1 → ×2), récolte contre
consommation (seuil réel) et PV du boss (total réel) ont tous un dénominateur honnête. Ceux-là
doivent être dessinés — et c'est aussi eux qui donnent à l'app son allure de jeu vidéo, qu'un
paragraphe ne donnera jamais.

Deux garde-fous complètent la règle.

**Garde-fou 1 — un dénominateur réel ne suffit pas s'il est variable** (§ 0 bis). L'effectif en est
un. Une jauge dont le *nombre de segments* dépend de l'effectif n'est pas une jauge, c'est une liste
déguisée : elle se lit à 6 et devient illisible à 20. Segments seulement quand leur nombre est fixe
(les cinq défis, les six étapes du tour) ; remplissage continu partout ailleurs.

**Garde-fou 2 — une accumulation ne se réduit jamais à un booléen.** C'est le cas que j'avais raté.
Un défi « 40 deathmatchs » passe six jours entre 0 et 40 ; c'est précisément la période où on le
consulte. Un état terminé/pas terminé répond « non » tout ce temps. Si la donnée est une
accumulation, elle se dessine en continu — c'est ce que font déjà les anneaux du classement, et
c'est pour ça qu'ils restent (§ 3.3).

### 4.1 La couleur encode un rôle, plus une catégorie

| Couleur | Signifie | Employée par |
| --- | --- | --- |
| `brand-500` amber | **la ville, l'objectif** | silhouette, population, palier, progression vers le palier |
| `accent-red` | **la menace** | boss et sa barre de vie, uniquement — jamais décliné par catégorie (§ B2 quater) |
| `accent-gold` | **le permanent** | matériaux, gains qui ne s'effacent jamais |
| `success` / `danger` | **l'état** | hausse/baisse, gagné/perdu, validé/non validé |
| gris (`text-*`, `surface-*`) | tout le reste | libellés, tables, tuiles de référence |

`accent-cyan`, `accent-pink`, `accent-violet` sortent des tuiles colonie : ces tuiles sont un
tableau de référence, elles n'ont pas besoin d'une identité chacune. Trois couleurs porteuses de
sens au lieu de huit couleurs porteuses d'étiquettes. C'est ce qui règle l'effet « tableur » de
`/campaign` sans rien changer au design system.

Les tiers de difficulté des défis (I→V, vert→rouge) restent : c'est une **échelle ordonnée à cinq
crans**, lue comme un dégradé, et elle a sa propre grille de lecture sur chaque écran. Les
catégories de boss, elles, n'en reçoivent pas (§ B2 quater) : trois valeurs ne font pas un dégradé,
elles se nomment plus vite qu'elles ne se décodent, et leur hexagone porte déjà une couleur —
celle de l'issue du combat.

### 4.2 Trois niveaux de panneau, et un seul « héros » par page

| Traitement | Rôle | Réservé à |
| --- | --- | --- |
| `bg-text-primary/4` + coin biseauté + filet gauche | une **ligne** | listes, tableaux, historiques |
| filet de section + hairline dégradé | une **section** | tout regroupement titré |
| dégradé + bordure d'accent | le **sujet de la page** | un seul bloc par page |

Aujourd'hui le troisième traitement sert aux cinq cartes de défi, à la carte boss et aux dix tuiles
colonie : il ne veut plus rien dire. Le réserver à la ville (accueil), au boss (`/week`), au
bandeau d'identité (`/players/:id`).

### 4.3 Les tableaux, resserrés

Tu les aimes, ils peuvent être meilleurs. Cinq réglages :

1. **Hauteur de ligne resserrée** (`py-5` → `py-3.5`, colonne d'espacement vide supprimée) : 64 px
   sur `/players`, **~72 px sur `/leaderboard`, où l'anneau de 44 px fixe le plancher**. On ne
   descend pas sous l'anneau : c'est lui qui porte l'avancement (§ 3.3).
2. **Une seule taille de chiffre par tableau**, en `tabular-nums`, alignée à droite — pour les
   colonnes de valeurs. Aujourd'hui `/leaderboard` mélange `text-lg` display et `text-2xs` mono
   dans une même ligne. Les anneaux ne comptent pas dans ce réglage : ce sont des jauges, pas des
   colonnes de chiffres, et ils ont leur propre traitement typographique.
3. **Le libellé vit dans l'en-tête de colonne, une fois.** Sous `lg`, les libellés sont répétés dans
   chaque cellule (`lg:sr-only`) : c'est correct en accessibilité mais ça double la hauteur de la
   carte mobile. Une carte mobile = deux lignes max : identité + une bande de chiffres légendés.
4. **Filet de 1 px entre les lignes** sur les tableaux denses, à la place du `gap-2` actuel. Le
   coin biseauté et le filet gauche suffisent à identifier la ligne.
5. **Pas de `opacity` comme porteur de sens.** Un état se dit avec un libellé et un regroupement.

### 4.4 Les états vides sont un état normal

Jour 6 de la campagne 1, c'est ce que l'app affiche pendant des semaines. Chaque `0` doit se lire
« pas encore », avec sa cible à côté :

| Aujourd'hui | À la place |
| --- | --- |
| `0 / 5` en 6xl | `Aucun défi validé — il en reste 5 avant dimanche` |
| Six lignes à `0` dégât | `L'escouade n'a pas encore joué cette semaine` en tête de tableau |
| `0` matériaux | `0 — le premier boss tombe dimanche, il en rapporte 240` |
| Historique : cinq tirets | `Première campagne. Le score s'inscrira au jour 71.` |

---

## 5. Plan d'exécution

Plan unique, intégrant la seconde passe du § 7. Chaque lot est livrable seul ; l'ordre est un ratio
impact / coût, pas une dépendance stricte — seuls les lots 0 et 1 en sont vraiment.

**Socle — à faire avant de maquetter**

| # | Lot | Contenu | Impact |
| --- | --- | --- | --- |
| 0 | **Effectif variable** | `CLAUDE.md` racine **corrigé** ; reste à borner `batteryView` et `presencePips`, et à vérifier chaque écran à 2 / 7 / 20 | **Préalable** — conditionne tout wireframe portant un élément par joueur |
| 1 | **Cycle de vie des campagnes** (§ F1) | Fin de l'ouverture paresseuse (`ensureRunFor`), états arrêtée / supprimée, identité par dates, écran `/admin/campaigns`, état de veille public | **Préalable** — sans lui, « aucune campagne » est inatteignable. Backend + front |

**Coût quasi nul, effet immédiat**

| # | Lot | Contenu | Impact |
| --- | --- | --- | --- |
| 2 | **Discipline de couleur et de jauge** | § 4.0 et § 4.1 partout, barre de boss rouge sur `/overview`, jauges sans dénominateur retirées des tuiles colonie | Élevé |
| 3 | **Vocabulaire** | Escouade partout ; « moral » et « campagne » tranchés ; **nuit → journée** (~12 entrées FR/EN + section `/rules`) ; **Stock → Réserve, Consommation → Bouches à nourrir, Présence → Escouade du jour** (§ 8.1) ; numéros de campagne remplacés par les dates, deux formes (§ 8.4) ; « en 6 temps » corrigé | Élevé |
| 4 | **Boss numérotés** | `BOSS 01..10` + label de catégorie, nom retiré de l'UI, catégorie en forme et non en couleur | Élevé — `resolveBossNumberLabel` existe déjà |
| 5 | **Statut de joueur** | « Hors campagne · rejoint la prochaine », groupe séparé sans opacité, `—` sur les colonnes de campagne, variante du bandeau de profil (§ A8) | Élevé — corrige un contresens visible |

**Le cœur du sujet**

| # | Lot | Contenu | Impact |
| --- | --- | --- | --- |
| 6 | **La ville** | Le composant silhouette, branché sur `ColonyView.ladder` et ses quatre bandes — **déjà écrites** (§ D1) | **L'objectif du jeu rendu visible** |
| 7 | **Accueil recomposé** | Ville + bloc « La journée » (réemploi de `presencePips`, déjà écrit) + résumé semaine + mini-classement ; podium déplacé | Élevé |
| 8 | **Fusion `/week`** | `/challenges` + carte boss, progression d'escouade sur les cartes | Élevé |
| 9 | **Fin de campagne** (§ F2) | Écran de dénouement : score, ville atteinte, courbe, dix boss, palmarès (§ F3). Se conçoit avec le lot 1, dont c'est l'autre extrémité | Élevé |

**Le reste**

| # | Lot | Contenu | Impact |
| --- | --- | --- | --- |
| 10 | **Tableaux resserrés** | § 4.3 sur `/leaderboard` et `/players`, colonne Matériaux | Moyen |
| 11 | **`/players` recentré** | Inversion des colonnes, pastille de présence, bandeau semaine sur le profil, **tri et filtre** (§ G1) | Moyen |
| 12 | **`/campaign` chronique** | Frise 10 nœuds sur nappe décorative, courbe permanente, économie repliée. **Le tiroir de boss est conservé intégralement** ; seul son point d'ouverture change | Moyen — le plus lourd |
| 13 | **Boucler l'historique** | Lien retour classement (semaine close) → nœud de campagne ; URL adressables (§ G2) | Faible |
| 14 | **Copy `/tour` + `/landing`** | Réécriture avec voix, 6ᵉ étape « ce qu'on attend de vous » | Moyen |
| 15 | **États vides** | § 4.4 sur tous les écrans, plus la distinction des trois zéros (§ G4) | Faible mais très visible en début de campagne |
| 16 | **Confort** | Détail d'un match (§ G3), comparaison de deux joueurs (§ F5), historique des synchros au backoffice (§ D3) | Faible, indépendants |
| 17 | **Demande du backend** | Endpoint catalogue de défis (§ F4), rappels (§ F6) | Hors périmètre maquettes |

---

## 6. Décisions arrêtées

Toutes tranchées le 29/08. Rien n'est plus en suspens : les maquettes peuvent partir sur cette base.

| # | Décision | Portée |
| --- | --- | --- |
| 1 | **L'effectif est variable.** Rien ne suppose sept. | § 0 bis — `CLAUDE.md` racine **corrigé**, reste à borner `batteryView` / `presencePips` |
| 2 | **`/challenges` fusionne dans `/week`**, avec le boss. | § 3.2 — nav à 6 entrées par échelle de temps, `/challenges` → redirection |
| 3 | **La courbe de population devient permanente.** | § 3.4 — sparkline et tiroir de courbe retirés, règle aussi le § C5 |
| 4 | **La carte devient une frise, sur nappe décorative.** | § 3.4 — nappe `aria-hidden`, non cliquable, bas contraste ; la géométrie passe à la frise |
| 5 | **Boss numérotés** `BOSS 01..10`, nom retiré de l'UI. | § B2 ter — `resolveBossNumberLabel` existe déjà |
| 6 | **Catégorie en label et en forme, pas en couleur.** | § B2 quater — la couleur reste à l'issue du combat |
| 7 | **« nuit » → « journée »** partout. | § B2 bis — ~12 entrées FR/EN + section `/rules` |
| 8 | **« moral »**, pas « attractivité ». **« campagne »**, pas « run ». | § B1 |
| 9 | **Les anneaux de défi restent**, et arrivent sur le profil. | § 3.3 / § 3.5 |
| 10 | **« Hors campagne · rejoint la prochaine »**, pas « inactif ». | § A8 — groupe séparé, sans opacité |
| 11 | **Le tiroir de boss est conservé intégralement.** | § 3.4 — seul son point d'ouverture change |
| 12 | **Pas de notion de joueur courant.** L'app reste à la troisième personne. | § 7.2 — proposition retirée |
| 13 | **Les campagnes ont un cycle de vie et un CRUD d'administration.** Rien n'est jouable tant qu'une campagne n'est pas lancée. | § F1 — emporte la fin de l'ouverture paresseuse et l'état de veille public |
| 14 | **Une campagne arrêtée prend la population du jour de l'arrêt comme score.** | § F1 — impose d'afficher la durée à côté du score |
| 15 | **La suppression emporte tout : les semaines couvertes disparaissent aussi.** | § F1 — c'est un `campaign-reset` restreint à une campagne ; les matchs et les profils survivent |
| 16 | **Une campagne s'identifie par ses dates, jamais par un numéro.** N'importe laquelle est donc supprimable, sans trou possible. | § F1 — `uk_run_first_week_start` est déjà unique ; entraîne des reformulations dans le lot 3. La numérotation des **boss** est inchangée |
| 17 | **Trois mots de la colonie changent** : Stock → Réserve, Consommation → Bouches à nourrir, Présence → Escouade du jour. Les quatre autres restent. | § 8.1 |
| 18 | **Renouvellement automatique activé par défaut.** | § 8.2 — « Arrêter » doit alors couper le renouvellement, sinon la pause n'existe pas ; l'état de veille devient rare, donc simple |
| 19 | **Une campagne arrêtée en milieu de semaine : la semaine en cours ne paie pas.** | § 8.3 — à écrire dans la confirmation d'arrêt |
| 20 | **Deux formes de date** : `24/08 → 01/11` en tableau, `Août – novembre 2026` en titre. | § 8.4 |
| 21 | **Le catalogue de défis a sa place réservée sur `/week`** ; les rappels n'en ont pas. | § 8.5 — l'endpoint viendra plus tard, l'emplacement est prévu |

**Par où commencer.** Le lot 6 (la ville) porte l'essentiel de l'enjeu — c'est lui qui rend
l'objectif du jeu visible — et il est à moitié écrit (§ D1). Les lots 2 à 5 (couleur, vocabulaire,
boss numérotés, statut de joueur) sont à coût quasi nul et rendent toutes les maquettes suivantes
plus faciles à lire. Les lots 0 et 1 sont les seuls vrais préalables : le premier conditionne tout
wireframe portant un élément par joueur, le second rend l'état « aucune campagne » atteignable.

---

## 7. Seconde passe — manques et angles morts

Analyse du 29/08 menée dans l'autre sens : plutôt que de regarder les écrans, comparer **ce que le
backend expose et ce que les modèles savent** avec ce que l'interface montre réellement. Trois
catégories : du travail déjà fait qui ne s'affiche nulle part, des fonctionnalités absentes, et des
détails à reprendre.

### 7.1 Du code écrit, jamais affiché

**D1 — La ville existe déjà dans le modèle. Sous le nom de `ladder`, et elle n'est rendue nulle
part.**

C'est la trouvaille de cette passe. `ColonyView.ladder` est un `computed` public qui produit un
`ColonyTierStepView[]` complet : pour chacun des douze paliers, son nom traduit, son seuil, les
matériaux restants, son état (franchi / courant / verrouillé), la progression vers le suivant — et
un **glyphe de silhouette**. Le mapping vit dans `colony-tier.utils.ts` :

```
CAMP, HAMLET                                 → CAMP
VILLAGE, BOROUGH, TOWN                       → HOUSES
CITY, RESIDENTIAL_QUARTER, GREAT_CITY,
METROPOLIS                                   → SKYLINE
MEGALOPOLIS, CAPITAL, CITADEL                → MONUMENT
```

Le commentaire du fichier dit exactement ce que je proposais au § 3.1 sans le savoir : *« the point
of the ladder is that it is one thing growing: a camp, then houses, then a skyline, then a
monument »*. Les clés `colony.ladder.title` / `.unit` / `.hint` existent aussi dans les
dictionnaires FR et EN.

Rien de tout ça n'apparaît à l'écran. Le panneau qui le rendait a été retiré, et les commentaires
de `campaign.html` y font toujours référence (« the ladder panel already carries it »).

→ **Le lot 6 (la ville) n'est donc pas un développement à partir de zéro** : la donnée, les états,
les libellés et la progression en quatre bandes sont écrits. Il manque le dessin. C'est le meilleur
rapport effort / impact de tout le document.

**D2 — `ColonyView.presencePips`**, déjà relevé au § 3.1 : un objet par joueur avec nom, initiales
et trois états, rendu nulle part. `/campaign` affiche à la place le `battery()` anonyme.

**D3 — L'historique des synchronisations.** Le backend expose `GET /api/admin/synchronizations`
(liste paginée) et `GET /api/admin/synchronizations/{id}` (le détail d'une exécution). Le front ne
consomme que `latest`. Un opérateur qui veut savoir *pourquoi* une semaine est fausse n'a donc accès
qu'à la dernière exécution, alors que l'historique complet est servi.

> **Constat de méthode :** trois view models publics construits et non rendus, c'est le signe que
> des panneaux ont été retirés au fil des refontes sans que leur source ne le soit. Avant de
> concevoir un écran, vérifier ce que `ColonyView` sait déjà faire.

### 7.2 Fonctionnalités absentes

**F1 — La campagne n'a pas de cycle de vie. Et aujourd'hui, elle s'ouvre toute seule.**

*(Exigence posée le 29/08 : rien ne doit être jouable tant qu'une campagne n'a pas été lancée depuis
l'administration ; elle démarre en renouvellement automatique ou manuellement ; on doit pouvoir
l'arrêter à un instant T, et la supprimer comme si elle n'avait jamais existé. Il faut donc un CRUD
de campagnes.)*

**Le point bloquant : c'est aujourd'hui l'inverse exact.** `RunService.ensureRunFor()` ouvre une
campagne **paresseusement, à la première consultation**. Le commentaire de `openRun()` le dit
lui-même : *« Several requests open a run lazily — the colony's three endpoints and the boss endpoint
all do — and the page fires them in parallel, so on the very first load two of them read "no run
yet" and both go on to insert »*. Autrement dit : **visiter le site crée une campagne.** Il n'existe
aucun moyen de ne pas en avoir une.

C'est le premier travail, et il n'est pas cosmétique : quatre endpoints publics dépendent de cette
ouverture implicite. Ils doivent apprendre à répondre « aucune campagne » au lieu d'en fabriquer une.

**Ce que le modèle sait faire, et ce qui lui manque.** `Run` porte `number`, `firstWeekStart`,
`lastWeekStart`, `rosterSize` (gelé à l'ouverture, délibérément) et `closedAt` — `null` tant qu'elle
court. Un seul booléen implicite, donc, là où l'exigence en demande cinq états :

| État | Existe ? | Ce qu'il implique |
| --- | --- | --- |
| **Aucune campagne** | ❌ impossible aujourd'hui | L'app publique passe en veille (voir ci-dessous) |
| **En cours** | ✅ `closedAt = null` | L'état actuel, le seul |
| **Terminée** (dix semaines écoulées) | ✅ `closedAt` posé par le rollover | Score = population du jour de solde |
| **Arrêtée à un instant T** | ❌ | ✅ **Tranché : son score est la population du jour de l'arrêt.** Voir ci-dessous |
| **Supprimée** | ❌ | Doit disparaître sans laisser de référence pendante |

**Sur la campagne arrêtée — tranché le 29/08 : elle prend la population du jour de l'arrêt comme
score.**

*Bonne nouvelle côté modèle :* rien de nouveau à stocker. La colonie n'est jamais avancée
incrémentalement, la campagne entière est rejouée depuis ses entrées persistées — la population d'un
jour quelconque est donc déjà calculable. Il suffit d'enregistrer **le jour de l'arrêt** et de lire
le score là, au lieu du jour de solde. Concrètement, `Run` gagne un `stoppedOn` (ou le score devient
un champ figé à l'arrêt) et le « jour de solde » cesse d'être la seule origine possible d'un score.

*Ce que ça coûte, et qu'il faut afficher :* la comparabilité. C'est la raison d'être de l'entité
`Run`, écrite dans son propre Javadoc — *« an act has no regular duration, so two campaigns fought in
two acts could never be compared; a run is exactly ten weekly rollovers, which makes every run
comparable to every other one by construction »*. Une campagne arrêtée en semaine 6 marquera presque
toujours moins qu'une campagne menée à dix semaines, non parce que l'escouade a moins bien joué,
mais parce qu'elle a eu moins de temps.

→ **L'historique des campagnes doit donc porter la durée à côté du score**, et marquer les campagnes
interrompues. Sans ça, `31` et `18` se lisent comme un classement alors que l'un couvre dix semaines
et l'autre six. C'est une modification du bloc « Les campagnes précédentes » du § 3.4 :

```
LES CAMPAGNES PRÉCÉDENTES
24/08 → 01/11  (en cours)   10 semaines     48
05/04 → 14/06   interrompue   6 semaines     18  ← la durée, sinon le score ment
25/01 → 05/04   complète     10 semaines     31
```

*Et une conséquence sur le geste :* arrêter fige un nombre. La confirmation doit donc l'annoncer —
« La population d'aujourd'hui, **34 habitants**, deviendra le score définitif de la campagne 1 » —
plutôt qu'un « Êtes-vous sûr ? ». C'est irréversible et c'est chiffrable : autant donner le chiffre.

**Sur la suppression — tranché le 29/08 : elle emporte tout, les semaines couvertes disparaissent
aussi.**

*La propriété rassurante :* ainsi définie, supprimer une campagne est exactement le
`campaign-reset` existant, **restreint aux semaines d'une campagne**. Or celui-ci est déjà décrit
comme *« an irreversible wipe of every record derived from match history »* — il efface le dérivé et
garde la source. La frontière est donc déjà tracée et n'a pas à être réinventée :

| Emporté | Conservé |
| --- | --- |
| La campagne elle-même (`Run`) | **Les matchs** (`ValorantMatch`, `PlayerMatch`) — ils viennent de Riot, ils ne sont pas de la donnée de campagne |
| Les boss de ses semaines (`WeeklyBossEncounter`, cascade `run_id`) | Les joueurs et leur statut |
| Les instantanés de colonie (`ColonyDailySnapshot`, cascade `run_id`) | Les saisons |
| Les défis tirés sur ses semaines et leur progression | Les profils : rang, historique, winrate, K/D, progression — **intacts** |
| Les classements de ses semaines | |

C'est ce qui rend la suppression acceptable : **aucun joueur ne perd son histoire de jeu**, seule
la partie disparaît. Et c'est cohérent avec le statut « hors campagne » (§ A8), dont toute la valeur
est justement de vivre en dehors d'une campagne.

*Deux pièges à traiter dans l'implémentation :*

1. **Le champion en titre peut disparaître.** `resolveChampionPlayerId` lit le classement de la
   dernière semaine close. Supprimer la campagne qui la contenait fait s'évanouir le champion —
   et avec lui le badge doré porté par le podium, le classement, l'escouade et les profils. Ce
   n'est pas un bug, mais l'absence doit être un cas prévu, pas un `null` qui traverse quatre
   composants.
2. **Le numéro de campagne doit disparaître de l'interface, au profit des dates.**
   ✅ **Tranché le 29/08.** Le problème : `nextRunNumber()` lit le plus grand numéro jamais ouvert
   et ajoute un, donc supprimer la campagne 2 de `{1, 2, 3}` laisse `{1, 3}` et la suivante
   s'appelle 4. « Campagne 1, Campagne 3 » fait se demander où est passée la 2 — exactement ce que
   la suppression prétendait éviter. Restreindre la suppression à la dernière campagne aurait
   contourné le symptôme ; **identifier une campagne par ses dates supprime la cause.** N'importe
   quelle campagne devient supprimable, sans trou possible, puisqu'il n'y a plus de séquence à
   trouer.

   *Le modèle le permet déjà :* `uk_run_first_week_start` est une contrainte d'unicité au même
   titre que `uk_run_number`. Une campagne est **déjà** identifiée de façon unique par son premier
   lundi. `number` peut soit être abandonné, soit rester une clé interne jamais affichée.

   *Deux formes à prévoir*, parce qu'une date est plus longue qu'un chiffre et que l'eyebrow du
   `page-header` tronque :

   | Forme | Usage | Exemple |
   | --- | --- | --- |
   | Courte | eyebrow, colonnes étroites, historique | `24/08 → 01/11` |
   | Longue | titre d'écran, confirmations | `Campagne du 24 août au 1er novembre 2026` |

   Une variante « mois » (`Août – novembre 2026`) est lisible et sans ambiguïté : deux campagnes
   sont séparées de dix semaines, elles ne peuvent pas partager le même mois de départ.

   *Reformulations que ça entraîne*, toutes dans le lot 3 (vocabulaire) :
   - `colony.eyebrow` (« Campagne · Run 1 · Semaine 1 sur 10 ») perd son numéro — et son `Run`, déjà
     signalé au § B1 ;
   - l'étiquette du § A8 devient **« rejoint la prochaine campagne »** au lieu de « rejoint la
     campagne 2 ». Meilleur de toute façon : un joueur ne retient pas un numéro, il retient que
     c'est la suivante ;
   - l'historique des campagnes s'ordonne par premier lundi décroissant, et sa colonne d'identité
     porte la plage de dates.

   *Ce qui n'est pas touché :* **la numérotation des boss** (§ B2 ter). `BOSS 01..10` compte les
   semaines **à l'intérieur** d'une campagne, via `weekIndexOf`. Elle repart à `01` à chaque
   campagne et ne peut pas être trouée, puisqu'une suppression emporte les dix d'un coup.

*Côté public :* un lien profond vers une semaine supprimée (`/leaderboard?week=…`) doit retomber sur
le sélecteur de semaine avec un message, pas sur une page vide.

**L'écran d'administration.** Une quatrième entrée `/admin/campaigns`, à côté d'Opérations, Joueurs
et Maintenance :

```
┌──────────────────────────────────────────────────────────────────────┐
│ CAMPAGNES                                                            │
├──────────────────────────────────────────────────────────────────────┤
│ RENOUVELLEMENT AUTOMATIQUE                          [ ●───  activé ] │
│ À la fin d'une campagne, la suivante s'ouvre le lundi qui suit.      │
│ Activé par défaut (§ 8.2). « Arrêter » le désactive, sinon la pause  │
│ demandée serait annulée dès le lundi suivant.                        │
├──────────────────────────────────────────────────────────────────────┤
│ EN COURS                                                             │
│ ┌──────────────────────────────────────────────────────────────────┐ │
│ │ 24/08 → 01/11     Semaine 4 / 10 · jour 25 / 71                   │ │
│ │ effectif gelé à 6 · 2 boss vaincus                                │ │
│ │                                    [ Arrêter ]  [ Supprimer ]     │ │
│ └──────────────────────────────────────────────────────────────────┘ │
├──────────────────────────────────────────────────────────────────────┤
│ TERMINÉES                          durée      score                  │
│ 14/06 → 23/08                    10 sem.        31   [ Supprimer ]   │
│ 05/04 → 14/06   interrompue       6 sem.        18   [ Supprimer ]   │
│                    ↑ pas de numéro : l'identité, c'est la plage de   │
│                      dates, donc aucune suppression ne fait de trou  │
├──────────────────────────────────────────────────────────────────────┤
│ [ + Lancer une campagne ]   ← désactivé tant qu'une campagne court   │
└──────────────────────────────────────────────────────────────────────┘
```

Quatre règles pour cet écran, toutes tirées de ce que le modèle impose déjà :

- **Une seule campagne ouverte à la fois.** `RunService` lit l'ouverte par `findByClosedAtIsNull()`
  — un `Optional`, donc un seul résultat attendu — et `uk_run_first_week_start` garantit qu'aucune
  campagne ne peut démarrer le même lundi qu'une autre. Le bouton « Lancer » est donc inerte tant
  qu'une campagne court — inerte et **expliqué**, pas simplement grisé.
- **Lancer, c'est choisir un lundi.** Une campagne commence forcément un lundi et dure dix
  rollovers. Le formulaire propose le lundi suivant par défaut et affiche la date de fin calculée
  ainsi que le jour de solde — les trois dates sont dérivées, autant les montrer.
- **Lancer gèle l'effectif.** `rosterSize` est figé à l'ouverture *« so an archive cannot rewrite the
  history of a run already played »*. L'écran doit donc afficher, avant confirmation : « 6 joueurs
  actifs seront gelés comme effectif de cette campagne ». C'est le moment où le statut des joueurs
  (§ A8) devient irréversible pour dix semaines — ça mérite d'être dit à ce moment-là, et nulle part
  ailleurs.
- **Arrêter et Supprimer sont deux actions, pas deux niveaux de la même.** Arrêter clôt une histoire
  et **fige un score** — la confirmation annonce le nombre. Supprimer nie qu'elle ait eu lieu, et
  passe par la confirmation par saisie que `ConfirmDialog` sait déjà faire pour `campaign-reset`.

**L'état « aucune campagne » côté public.** C'est un écran que je n'avais pas prévu, et ce n'est
**pas** un état vide au sens du § 4.4 : là il s'agissait de zéros en début de campagne, ici la partie
n'a pas commencé. Tout ne s'éteint pas pour autant — la distinction utile est entre **le jeu vivant**
et **l'archive** :

| Écran | Sans campagne |
| --- | --- |
| `/` la colonie | **En veille.** Pas de ville, pas de journée : un écran unique qui dit qu'aucune campagne n'est ouverte, avec la dernière et son score |
| `/week` | **En veille.** Ni boss ni défis en cours |
| `/leaderboard` | **Archive lisible.** Les semaines closes gardent leur classement ; seule la semaine en cours disparaît |
| `/campaign` | **Archive lisible.** La frise de la dernière campagne, les campagnes précédentes, les courbes |
| `/players` et les profils | **Intacts.** Rang, historique de matchs, progression ne dépendent pas d'une campagne — c'est déjà ce qui fait la valeur du statut « hors campagne » (§ A8) |
| `/rules` | **Intact.** |

La règle : **ce qui est mesuré s'arrête, ce qui est enregistré reste consultable.** Un visiteur qui
arrive entre deux campagnes doit pouvoir lire ce qui s'est passé, pas trouver porte close.

**F2 — Une campagne se termine sans rien.**

Le modèle sait pourtant finir : `Colony.settlementDay` et `Colony.finalPopulation` — *« the run's
score: the population of its settlement day, once the tenth week's materials and boss are
credited »*. Dix semaines de jeu aboutissent aujourd'hui à **une ligne de plus dans une liste**.

Il manque le moment. Un écran de fin de campagne : le score final, la silhouette de la ville
atteinte (D1), la courbe complète, les dix boss et leur issue, le champion de chaque semaine, la
comparaison avec la campagne précédente. C'est la récompense de dix semaines, et c'est aussi ce qui
donne envie d'enchaîner sur la suivante.

**F3 — Aucun palmarès.** `resolveChampionPlayerId` ne donne que le champion **en titre**. Dix
semaines produisent dix champions, et `rankings/history` les sert déjà, paginés. Personne ne peut
voir qui a gagné quelle semaine. Un bloc de palmarès sur `/leaderboard` ou sur l'écran de fin de
campagne coûte une lecture de données déjà disponibles.

**F4 — Le catalogue de défis est invisible.** 63 entrées actives, cinq tirées chaque lundi. Aucun
endpoint public ne les expose, donc aucun écran ne peut montrer ce qui *peut* tomber. Or c'est
exactement ce qui fait patienter entre deux tirages, et ce qui permet à un joueur de s'entraîner sur
ce qui reviendra. Demande un endpoint côté backend — le seul point de cette liste qui ne soit pas
purement frontend.

**F5 — Rien ne compare deux joueurs.** La vue Progression compare plusieurs saisons **d'un même
joueur** (jusqu'à cinq séries, palette validée). Le même composant, alimenté par deux joueurs sur
une saison, donnerait la comparaison qui manque — c'est le premier réflexe d'un coach.

**F6 — Rien ne fait revenir.** Jeu à échéance hebdomadaire, sans aucun rappel : ni notification, ni
résumé, ni « le boss tombe dans 6 h et il lui reste 12 % ». Le compte à rebours n'existe que si on
ouvre la page. Hors périmètre maquettes, mais à noter : c'est le mécanisme de rétention central de
ce type de jeu, et il est absent.

### 7.3 Détails à reprendre

**G1 — `/players` n'a ni recherche, ni tri, ni filtre.** Zéro champ de saisie sur la page. Acceptable
à 7, cassé à 20 — et l'effectif variable vient d'être acté (§ 6, décision 1). Le tri par colonne est
le minimum ; le filtre par statut (dans la campagne / hors campagne) découle du § A8.

**G2 — Une seule chose est adressable par URL.** `?week=` sur le classement, et c'est tout. Ni un
défi, ni un boss, ni une campagne passée. Impossible de coller un lien dans le Discord de l'escouade
pour dire « regarde la semaine 3 ». Pour une app d'équipe, c'est un manque de partage plus que de
navigation.

**G3 — Les lignes de match ne mènent nulle part.** L'historique est riche (carte, agent, K/D/A, ADR,
ACS, dégâts) et c'est la partie de l'app que tu préfères — mais une ligne n'est pas cliquable. Le
détail d'un match (score par round, composition, timeline) est la profondeur naturelle de cet écran,
si l'API Henrik le permet.

**G4 — La fraîcheur des données tient dans un point de 6 px.** Le pied de la sidebar porte un point
vert ou rouge et un horodatage. Sur une app qui ne fait qu'afficher des données synchronisées, une
synchro en échec devrait se voir sur les écrans concernés, pas seulement dans un coin. Et la
fraîcheur est **globale** : si la synchro d'un seul joueur a échoué, son `0` est indiscernable d'un
vrai `0` — ce qui, combiné au § C6 et au § A8, fait trois raisons différentes d'afficher un zéro
sans jamais dire laquelle.

### 7.4 Où atterrissent ces constats

Il n'y a **qu'un seul plan**, celui du § 5 : les constats de cette passe y sont intégrés plutôt que
de former une seconde liste concurrente. Correspondances :

| Constat | Lot du § 5 |
| --- | --- |
| F1 — cycle de vie des campagnes | **Lot 1**, socle, avec le lot 0 |
| D1 — brancher `ladder` | **Lot 6**, la ville |
| D2 — `presencePips` | **Lot 7**, accueil recomposé |
| F2 + F3 — fin de campagne et palmarès | **Lot 9** |
| G1 — tri et filtre sur l'escouade | **Lot 11** |
| G2 — URL adressables | **Lot 13** |
| G4 — les trois zéros | **Lot 15** |
| D3, G3, F5 — confort | **Lot 16** |
| F4, F6 — demandent du backend | **Lot 17** |

**`/admin/campaigns` est maquettable en l'état** : les cinq états du cycle de vie, le sort d'une
campagne arrêtée, le périmètre d'une suppression et l'identité par dates sont tous tranchés (§ 6,
décisions 13 à 16). Les points encore ouverts sont ailleurs, et listés au § 8.

---

## 8. Les cinq derniers points — tranchés

Tous arbitrés le 29/08. Le document ne contient plus aucune question ouverte.

### 8.1 Lexique de la colonie — trois mots changent, quatre restent

✅ **Tranché.** Le § B2 relevait que **Stock · Consommation · Surplus · Efficacité · Présence ·
Attractivité · Arrivées · Matériaux** est un glossaire de bilan comptable. On ne remplace pas les
huit — seulement ceux dont le mot de gestion n'apprend rien :

| Aujourd'hui | Devient | Pourquoi |
| --- | --- | --- |
| Stock | **Réserve** | « Stock » est un mot d'inventaire ; une réserve se garde et s'épuise, ce que fait la nourriture |
| Consommation | **Bouches à nourrir** | Nomme la cause (les habitants) plutôt que l'opération comptable |
| Présence | **Escouade du jour** | Dit ce qui est compté — qui est là — au lieu d'un état abstrait |
| Attractivité | **Moral** | Décision 8 : nomme la cause, pas la conséquence |
| Surplus · Efficacité · Arrivées · Matériaux | *inchangés* | Exacts ; un synonyme « de jeu » les rendrait plus vagues, pas plus clairs |

Un changement partiel plutôt qu'un thésaurus complet : le lecteur n'a que trois mots à réapprendre,
et `/rules` reste lisible pour un coach.

### 8.2 Renouvellement automatique — **activé** par défaut

✅ **Tranché : activé.** *(Contre ma recommandation, qui allait vers « désactivé » ; l'app tourne
donc toute seule, comme aujourd'hui, et l'administration n'intervient qu'en exception.)*

**Deux conséquences à intégrer aux maquettes :**

1. **« Arrêter » ne suffit pas à faire une pause.** Renouvellement actif, arrêter une campagne en
   ouvre une autre le lundi suivant — presque certainement pas ce que veut un opérateur qui vient
   de cliquer « Arrêter ». Deux façons de traiter ça, à choisir au moment de l'écran : soit
   **arrêter désactive le renouvellement** (et le dit), soit la confirmation prévient — « la
   prochaine campagne s'ouvrira lundi ; désactivez le renouvellement pour marquer une pause ».
   Ma préférence va à la première : le geste correspond à l'intention.
2. **L'état de veille devient rare — donc il reste simple.** Il ne survient qu'à l'installation ou
   après un arrêt volontaire avec renouvellement coupé. Ne pas y investir un écran élaboré : un
   bloc unique qui dit qu'aucune campagne n'est ouverte, la dernière et son score, et c'est tout.
   Le tableau du § F1 (ce qui s'éteint / ce qui reste en archive) suffit à le spécifier.

*Effet secondaire heureux :* l'enchaînement automatique gèle un nouvel effectif à chaque campagne,
ce qui donne son sens littéral à l'étiquette « rejoint la prochaine » (§ A8) — un joueur activé en
cours de route entre vraiment au lundi suivant la fin.

### 8.3 Campagne arrêtée en milieu de semaine — la semaine en cours ne paie pas

✅ **Tranché.** Arrêter un mercredi laisse un boss ni vaincu ni survivant : il ne rapporte rien et
ne coûte rien. C'est la lecture littérale de « population du jour de l'arrêt » (décision 14), c'est
déjà ce que la relecture de la campagne calcule, et ça évite d'inventer un demi-boss.

→ **À écrire dans la confirmation d'arrêt**, sous le score figé : « la semaine 7, en cours, ne sera
pas comptée ».

### 8.4 Dates de campagne — deux formes, par contexte

✅ **Tranché.**

| Forme | Où | Exemple |
| --- | --- | --- |
| Courte | tableaux, eyebrow du `page-header`, historique, colonnes étroites | `24/08 → 01/11` |
| Longue | titres d'écran, confirmations, écran de fin de campagne | `Août – novembre 2026` |

La courte est précise et tient dans une colonne ; la longue se retient et se prononce. C'est le
couple que le § F1 proposait déjà.

### 8.5 Périmètre backend — la place est réservée pour le catalogue, pas pour les rappels

✅ **Tranché.** Le catalogue de défis (§ F4) reçoit un emplacement dès les maquettes : sur `/week`,
sous les cinq cartes, une bande « ce qui peut tomber la semaine prochaine ». Ça coûte une ligne de
maquette aujourd'hui et évite de rouvrir la page quand l'endpoint arrivera.

Les rappels (§ F6) n'ont rien à réserver : ce n'est pas un écran. Ils restent au lot 17.
