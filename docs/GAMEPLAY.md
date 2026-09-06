# ValoQuests — Règles du jeu

## Vue d'ensemble

Une escouade tient une **base**, et un **vaisseau** se construit en son centre. Pendant **dix
semaines**, elle part chercher les **blessés** abandonnés sur dix planètes : chaque semaine, une
planète, un groupe de blessés à évacuer, et un **gardien** qui les retient au sol tant que l'escouade
n'a pas percé ses lignes.

Tout ce que les joueurs jouent dans Valorant alimente la base. Le score de la campagne est la
**taille de la base**. Le nombre de gardiens vaincus décide de l'état final de la fusée.

| Boucle | Rythme | Ce qui se passe |
|---|---|---|
| Synchronisation | toutes les 30 minutes | Les parties arrivent, les stocks montent, le gardien encaisse |
| Quotidienne | à minuit | La base mange, la journée se ferme |
| Hebdomadaire | lundi au dimanche | On abat le gardien et on sauve les survivants |
| Campagne | dix semaines | La fusée se construit, le score final se fige |

---

## Les deux ressources

Chaque partie produit les deux en même temps, réparties selon le mode joué.

| Mode | Nourriture | Composants |
|---|---|---|
| Compétitif, Premier, Non classé (même en solo) | 30 % | **70 %** |
| Deathmatch, Team Deathmatch, Spike Rush, Skirmish | **70 %** | 30 % |

Le montant réparti est la valeur de dégâts de la partie :

| Mode | Défaite | Nul | Victoire | Durée moyenne |
|---|---|---|---|---|
| Compétitif, Premier | 350 | 425 | 500 | 35 min |
| Non classé | 320 | 390 | 460 | 33 min |
| Team Deathmatch | 110 | 135 | 160 | 10 min |
| Spike Rush | 110 | — | 150 | 9 min |
| Deathmatch | 100 | — | 150 | 9 min |
| Skirmish | 90 | 110 | 130 | 6 min |

Les valeurs sont calées pour que **l'heure de jeu rapporte à peu près la même chose quel que soit le
mode**, rendements décroissants compris : entre 680 et 780 dégâts par heure pour une session d'une
heure. Le Non classé, le Spike Rush et le Skirmish ont été réglés sur ce critère par rapport au
barème d'origine du projet (280 / 340 / 400, 130 / 180 et 120 / 145 / 170).

### Ce que chaque ressource sert à faire

```
Nourriture   ->  NOURRIR la base chaque jour
             et INSTALLER les rescapés du dimanche
Composants    ->  ATTEINDRE les survivants (véhicules, matériel d'expédition)
```

**Ce sont de vrais stocks.** Rien n'est remis à zéro : ce qui n'a pas été dépensé reste en réserve
d'une semaine sur l'autre. C'est ce qui permet de mettre de côté avant une grosse semaine.

### Exemples concrets

```
Compétitif gagné   (500)  ->  150 nourriture  +  350 composants
Compétitif perdu   (350)  ->  105 nourriture  +  245 composants
Deathmatch gagné   (150)  ->  105 nourriture  +   45 composants
Deathmatch perdu   (100)  ->   70 nourriture  +   30 composants
```

Par partie, le compétitif rapporte plus des deux. C'est à l'heure de jeu que l'arbitrage existe : une
partie compétitive dure environ 35 minutes, un deathmatch environ 9.

---

## Les deux multiplicateurs

Ils s'appliquent à toute partie et doivent être **affichés explicitement** au joueur.

**Rendements décroissants**, contre le farm, déjà présents dans le projet :

| Partie du jour | 1 à 5 | 6 à 9 | 10 et + |
|---|---|---|---|
| Valeur | 100 % | 50 % | 25 % |

**Série de jours consécutifs**, pour récompenser l'effort quotidien :

| Jours d'affilée | 1 | 2 | 3 | 4 | 5 | 6+ |
|---|---|---|---|---|---|---|
| Bonus | 0 % | +2 % | +4 % | +6 % | +8 % | **+10 %** |

Le premier jour ne donne rien : un bonus que tout le monde a n'est pas un bonus. Le plafond est
volontairement bas, pour qu'un joueur qui saute une journée puisse encore rattraper le premier du
classement.

Les règles de la série, dans le détail :

1. La série est **individuelle** et remise à zéro à minuit dès qu'un joueur saute une journée.
2. Une journée compte si le joueur y a joué **au moins une partie valorisée** : un mode qui vaut
   zéro, comme le Swiftplay, ne fait pas de journée.
3. Dès qu'une partie valorisée existe dans la journée, la journée compte et **toutes les parties de
   cette journée** prennent le bonus du nouveau compteur. C'est provisoire jusqu'à minuit, comme le
   reste. Il n'y a qu'un bonus par journée : il ne monte pas partie après partie.
4. Une journée est le jour calendaire du fuseau du projet, celui de **l'heure de début** de la
   partie.
5. Le compteur ne s'arrête jamais : ni au lundi, ni entre deux campagnes. Il est calculé sur
   l'historique de calibration aussi, et un joueur peut donc déjà être à six jours le lundi
   d'ouverture. L'affichage continue de compter au-delà de six, « 12 j », mais le bonus reste à
   +10 %.
6. Le bonus s'applique à tout ce qu'une partie produit : dégâts au gardien, composants, nourriture,
   croissance de la base. Jamais aux défis.

Les deux multiplicateurs se multiplient : valeur de la partie × rendement décroissant × (1 + bonus
de série), arrondi à l'entier une seule fois, à la fin, par partie.

Où ils s'affichent : la série sur chaque ligne d'opérateur (« × 1,08 · 5 j », et « à jouer
aujourd'hui » tant que la journée n'a pas encore compté) ; les rendements
décroissants sous les dégâts du jour de chaque opérateur dès qu'ils s'appliquent (« 6 parties, 1 à
50 % »), avec la règle rappelée en une ligne sous le tableau de l'escouade.

---

## Le rythme de mise à jour

L'application interroge Riot **toutes les 30 minutes**. Chaque synchronisation importe les parties
terminées depuis la précédente, puis **rejoue la campagne entière** depuis ses données d'origine. Rien
n'est incrémenté, tout est recalculé : une synchronisation peut être rejouée sans jamais fausser un
total.

Mis à jour **à chaque synchro**, sans attendre le soir :

- la nourriture et les composants produits,
- les points de vie du gardien et l'avancement sur lui,
- la progression des défis du jour et de la semaine,
- la taille de la base, à titre provisoire pour la journée en cours.

Figé **à minuit**, quand la journée se ferme :

- le repas de la base, et la famine si le stock n'a pas suivi,
- la journée comptée ou perdue dans la série de jours consécutifs,
- la résolution du défi du jour,
- la ligne de la journée, qui ne bouge plus ensuite.

Concrètement : une partie terminée à 14 h 10 est prise en compte au plus tard à 14 h 30, la barre du
gardien descend dans la foulée et les deux stocks montent d'autant.

### Ce que « provisoire » veut dire

Les rendements décroissants dépendent du **rang de la partie dans la journée**. Ce rang se calcule
par **valeur décroissante**, pas par heure : les meilleures parties du jour gardent toujours 100 %,
et une partie d'échauffement en Deathmatch ne dévalue jamais la compétitive jouée ensuite. Tant que
la journée n'est pas finie, une partie qui remonte peut faire passer une partie moins bonne au
palier inférieur et faire **légèrement reculer** un total déjà affiché. C'est définitivement
tranché à minuit.

Le coup fatal au gardien est attribué à la **partie** qui a fait passer ses points de vie sous zéro,
jamais à la synchronisation qui l'a découverte : l'heure retenue est celle de la partie.

---

## La journée

Une journée produit en continu et se clôt à minuit, dans cet ordre :

**1. La base grandit** (à chaque synchro). Tout ce qui a été joué la fait grandir, quel que soit le
mode. C'est la seule source de croissance quotidienne, et elle est volontairement neutre par mode :
aucun mode n'est un mauvais choix pour le score.

**2. Le gardien encaisse** (à chaque synchro). Chaque partie lui inflige ses dégâts dès qu'elle
remonte, sans attendre la fin de la journée.

**3. La base mange** (à minuit). Chaque habitant consomme de la nourriture. Tant que le stock suit,
tout va bien. S'il se vide, les habitants qui ne mangent pas commencent à mourir.

**4. Le défi du jour** se résout à la clôture. Les blessés qu'il ramène sont **acquis quoi qu'il
arrive**, et comptés pour le dimanche : ils partent avec le vaisseau.

### Ce que coûte une grosse base

L'entretien monte avec la population, doucement et volontairement :

| Semaine | 1 | 3 | 5 | 7 | 9 | 10 |
|---|---|---|---|---|---|---|
| Part de votre nourriture absorbée par l'entretien | 0,7 % | 2,6 % | 5,1 % | 7,2 % | 9,6 % | **11 %** |

Une base de dix mille habitants demande plus d'attention qu'un campement de mille, sans que
l'entretien ne devienne jamais le sujet principal.

En jeu normal la base ne meurt jamais de faim : tant qu'il reste de la nourriture en réserve, chaque
habitant mange, et le vaisseau laisse toujours sept repas de côté le dimanche. La famine ne commence
que lorsque **la réserve est vide**, donc après plus d'une semaine sans aucune partie. Chaque soir
sans nourriture, **5 % des habitants non nourris** meurent :

| Soirs de famine | 1 | 3 | 5 | 7 |
|---|---|---|---|---|
| Habitants perdus | 5 % | 14 % | 23 % | **30 %** |

Le vrai prix de l'arrêt, c'est le gardien : une semaine sans jouer le laisse debout à 0 %, et il
prend **35 % de la base** (voir « Si le gardien survit »). La famine ne fait que s'y ajouter quand
la réserve est vide.

---

## La semaine

### Le gardien, en direct

Le gardien a des points de vie fixés au lundi. **Toute partie jouée par n'importe quel joueur actif
lui inflige ses dégâts**, dès la synchronisation suivante, donc à trente minutes près. Il peut tomber le
mardi à midi comme le dimanche soir, et sa barre descend pendant qu'on joue.

Le combat est **strictement insensible au mix de modes** : c'est le total joué qui compte.

Seules les parties lui font des dégâts. **Un défi validé ne lui en fait aucun** : les défis
ramènent des blessés, les parties ouvrent la percée.

Quand il tombe, l'application retient **qui a porté le coup fatal et sur quelle partie**.

### Après sa mort

La semaine n'est pas finie. Les jours restants continuent de remplir les deux réserves et de faire
grandir la base. Tuer le gardien tôt est un avantage, jamais une fin de semaine.

### Le règlement du dimanche

```
Blessés des défis = ce que les défis de la semaine ont ramené,
                    jamais plus que le groupe
Extraction        = le plus petit de ces trois nombres
                      le reste du groupe (groupe − blessés des défis)
                      ce que les composants permettent d'ATTEINDRE
                      ce que la nourriture permet d'INSTALLER
                    multiplié par l'avancement sur le gardien
Sauvés            = blessés des défis + extraction
```

Les blessés des défis **font partie du groupe repéré** sur la planète : ils ne s'ajoutent pas
au-dessus. Ils partent les premiers, sans dépenser de composants ni de nourriture et sans subir
l'avancement : les opérateurs sont allés les chercher eux-mêmes. Le vaisseau s'occupe du reste. Une
semaine où tout le groupe est ramené, les défis ne ramènent donc personne de plus, mais ils
économisent les composants et la nourriture que ces blessés auraient coûtés.

L'avancement vaut 1 si le gardien est tombé. Sinon il vaut la part de ses points de vie retirés : un
gardien repoussé à 70 % laisse extraire 70 % de ce qu'on aurait pu.

Les composants et la nourriture ne sont dépensés que pour les blessés **effectivement extraits** :
14 composants et 12 nourriture par blessé à bord, après l'avancement. Ce qui est dépensé l'est
**pour de bon**, le reste demeure en réserve.

**Le vaisseau ne touche jamais aux sept prochains repas.** Avant d'extraire, la nourriture des sept
soirs à venir est mise de côté ; seule la nourriture au-delà de cette réserve paie des lits. Sans
cette règle, une escouade qui joue le week-end vidait sa nourriture le dimanche et mourait de faim
du lundi au vendredi.

### L'ordre du dimanche soir

À minuit, dans cet ordre : le défi du jour se résout, le gardien attaque s'il est encore debout,
puis le vaisseau revient et les blessés sauvés rejoignent la base. Les blessés ramenés ne subissent
donc jamais l'attaque du gardien : ils n'étaient pas encore là.

### Si le gardien survit

Il attaque la base. Les pertes sont **continues et quadratiques**, sans aucun seuil :

```
pertes = base x (1 - avancement)² x 35 %
```

| Avancement | 99 % | 93 % | 84 % | 70 % | 20 % |
|---|---|---|---|---|---|
| Pertes | 0,004 % | 0,2 % | 0,9 % | 3,2 % | 22 % |

Rater de peu ne coûte quasiment rien. Ne rien faire coûte cher.

### Le lundi

Nouveau groupe, nouveau gardien. Les réserves de nourriture et de composants sont **conservées**. Une mauvaise
semaine ne se paie jamais la suivante.

Le roster ne change pas : l'administrateur ne peut ni retirer ni ajouter un opérateur pendant une
campagne. Une désactivation ou un archivage demandé en cours de route prend effet à la fin de la
campagne, et un joueur INACTIVE ne devient ACTIVE qu'à l'ouverture de la suivante.

### Qui compte

Seuls les joueurs **ACTIVE au lundi d'ouverture** de la campagne comptent : pour les dégâts au
gardien, les deux ressources, la croissance de la base et les blessés des défis. Un joueur INACTIVE
reste synchronisé et voit sa **progression sur chaque défi**, du jour comme de la semaine, à titre
purement indicatif : il mesure à quelle vitesse il irait, sans rien apporter ni rien coûter au jeu.

---

## La campagne

Dix semaines, chacune avec sa forme propre, décrite par deux nombres exprimés en part de la référence
de l'escouade.

| Semaine | 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 | 9 | 10 |
|---|---|---|---|---|---|---|---|---|---|---|
| Catégorie | MINEUR | STD | STD | STD | **ELITE** | MINEUR | STD | STD | STD | **ELITE** |
| Gardien | 0,60 | 0,80 | 0,95 | 0,85 | 1,30 | 0,60 | 1,00 | 0,90 | 0,95 | 1,35 |
| Groupe | 1,00 | 1,30 | 0,90 | 1,10 | 1,50 | 1,20 | 0,80 | 1,10 | 1,00 | **2,00** |

Cela produit des semaines qui se vivent différemment sans qu'aucune règle ne change :

- **semaine 6** : gros groupe, gardien faible. Une course au ravitaillement.
- **semaine 7** : petit groupe, gardien coriace. Un siège.
- **semaine 10** : le plus gros groupe de la campagne derrière le plus gros gardien.

### Les récompenses montent

Les groupes de survivants et les rescapés des défis progressent de **+4 % par semaine**, en
progression **linéaire** : × 1,00 en semaine 1, × 1,04 en semaine 2, × 1,36 en semaine 10. Jamais
composée.

Combinée à l'entretien qui monte de dix points, la campagne se durcit progressivement tout en payant
mieux.

---

## Le cycle de vie d'une campagne

L'administrateur **ouvre** la campagne depuis le backoffice. À cet instant, le roster est gelé et la
calibration est calculée. La campagne **démarre le lundi suivant** et dure dix semaines. Elle se
**clôt** après le règlement du dixième dimanche, et son score final se fige.

Entre deux campagnes, il n'y a ni gardien, ni base, ni réserves qui bougent : seul le **classement
hebdomadaire** continue de tourner. Les réserves de nourriture et de composants ne se reportent pas
d'une campagne à l'autre : chaque campagne repart de zéro.

---

## La calibration et les paliers

Avant le lancement, l'application lit **neuf mois d'historique** du roster gelé. Pour chaque joueur
actif, elle calcule la **moyenne de ses dégâts hebdomadaires** sur la fenêtre ; la référence de la
campagne est la **moyenne de ces moyennes**, avec un **plancher de 2 000**. Des moyennes, parce que
le gardien vaut « référence × joueurs » : c'est une somme que l'on vise, et un joueur très fort doit
y peser.

Toutes les semaines de la fenêtre comptent, **y compris celles où le joueur n'a rien joué** : elles
valent zéro dans sa moyenne. Un joueur qui joue peu est un joueur faible, et la référence doit le
refléter. Pas de médiane : mesurée le 04/09/2026 sur le roster réel, la médiane avec les semaines
vides tombait à 396 par joueur, soit une compétitive par semaine, parce que la moitié du roster joue
une semaine sur deux. La moyenne donnait environ 1 050.

Le plancher de 2 000 correspond à l'exemple Amateur ci-dessous, quatre compétitives et trois parties
rapides par semaine. En dessous, un gardien tomberait en une soirée et le jeu n'aurait plus d'objet :
la première campagne d'une escouade irrégulière se joue donc au plancher, et c'est voulu.

Si l'historique d'un joueur ne couvre pas neuf mois, la fenêtre est **réduite d'un mois à la
fois, pour tout le monde**, jusqu'à ce que chaque joueur soit couvert. Un joueur est couvert quand
sa plus ancienne partie connue est antérieure au début de la fenêtre. Un joueur qui a **moins d'un
mois** d'historique est un débutant : il ne fait pas réduire la fenêtre et prend la médiane de son
escouade. Sondé le 04/09/2026 : Henrik rend l'historique jusqu'à juillet 2024 pour le roster, à
30 requêtes par minute ; le walker actuel, qui ne remonte que deux actes, est à remplacer par une
lecture de toute la fenêtre.

> **Point critique.** La référence doit être calculée avec **exactement les mêmes multiplicateurs**
> que pendant la campagne, série de jours consécutifs et rendements décroissants compris. Les
> appliquer d'un côté et pas de l'autre décale la barre du gardien, d'environ 30 % avec l'ancien
> plafond de +50 %, ce qui faisait passer une escouade régulière de 8 gardiens vaincus à 6,8 sans
> que personne ne comprenne pourquoi.

Elle fixe la taille des gardiens, celle des groupes de survivants et la valeur des défis. Elle est
calculée **une seule fois** et **plus jamais recalculée** : rien n'est ajustable une fois la campagne
lancée.

Un joueur sans historique est un débutant et prend la médiane de son escouade.

De cette référence découle le **palier**, affiché sur la campagne :

| Palier | Référence hebdomadaire par joueur | Exemple |
|---|---|---|
| Amateur | moins de 3 500 | 4 compétitifs et 3 rapides par semaine |
| Normal | 3 500 à 9 000 | 7 compétitifs et 9 rapides |
| Confirmé | 9 000 à 16 000 | 16 compétitifs et 25 rapides |
| Élite | plus de 16 000 | 28 compétitifs et 35 rapides |

Le palier existe pour que deux escouades de niveaux très différents puissent comparer leurs campagnes.
Une base de 30 000 en palier Normal et une base de 119 000 en palier Élite se lisent côte à côte, et
toutes deux auront vaincu environ 8 gardiens sur 10.

**Toutes les grandeurs sont exprimées par joueur actif**, ce qui rend le jeu identique à 2 comme à 20
joueurs. Le roster est gelé à l'ouverture de la campagne et ne peut plus changer, pas même par le
backoffice.

---

## Les défis

**Un défi quotidien**, commun à l'escouade, tiré chaque matin dans le pool des **21 défis
quotidiens** de `CHALLENGES-CATALOGUE.md`, sans répétition sur 21 jours. Il se résout le soir même.

**Cinq défis hebdomadaires**, un par difficulté, tirés le lundi dans les **100 défis hebdomadaires**
du même catalogue. Les règles d'écriture sont dans `CHALLENGES.md`.

Les deux ramènent des blessés **acquis quoi qu'il arrive** : ils partent les premiers le dimanche,
avec le vaisseau, sans dépenser de ressources et sans subir l'avancement sur le gardien (voir « Le
règlement du dimanche »). Ils font partie du groupe de la semaine, jamais plus. Aucun défi ne fait
de dégâts au gardien. C'est ce qui en fait un bonus fiable plutôt qu'un pari.

### Le barème

Chaque défi validé ramène des survivants **pour le joueur qui l'a validé**. La valeur est
proportionnelle à la référence de la campagne, ce qui la rend identique en poids relatif pour une
escouade d'amateurs comme pour des pros.

```
survivants = référence x poids / 1000
```

| Défi | Poids | Survivants par joueur (référence 5 300) |
|---|---|---|
| Défi du jour | 1,2 | 6 |
| Hebdomadaire EASY | 1,0 | 5 |
| Hebdomadaire NORMAL | 1,7 | 9 |
| Hebdomadaire MEDIUM | 2,7 | 14 |
| Hebdomadaire HARD | 3,9 | 21 |
| Hebdomadaire VERY_HARD | 5,4 | 29 |

Une semaine parfaite, sept défis du jour et les cinq hebdomadaires, vaut **120 survivants par
joueur**. Les valeurs progressent comme le reste des récompenses, de +4 % par semaine.

### Ce qu'ils pèsent

Le poids des défis dans le score final est **à resimuler** avec la règle ci-dessus : les chiffres de
la version précédente supposaient des blessés ajoutés au-dessus du groupe et ne sont plus valables.
L'intention ne change pas : un bonus substantiel qui n'est jamais un passage obligé, dans la même
proportion à tous les paliers.

### Le classement hebdomadaire

Le classement individuel de la semaine additionne deux choses : les **dégâts au gardien** du joueur
sur la semaine, multiplicateurs compris, et les **blessés ramenés par ses défis validés**, un point
par blessé. Les dégâts des défis et le bonus d'équipe disparaissent ; la régularité est déjà payée
par la série.

```
points d'un défi = blessés qu'il ramène = référence x poids / 1000 x progression
```

Soit, pour une référence de 5 300 en première semaine : 6 pour le défi du jour, puis 5 / 9 / 14 /
21 / 29 par difficulté. Une semaine parfaite de défis vaut environ 120 points, contre 5 300 de
dégâts pour une semaine médiane : les défis départagent, ils ne font pas le classement. Un seul
chiffre par défi sur les deux piliers, c'est ce qui rend la lecture simple.

Le classement affiche, pour chaque joueur, **l'état d'avancement de chaque défi** en cours, du jour
comme de la semaine.

---

## Les titres

Quatre titres décernés chaque semaine, pour que la reconnaissance ne se concentre pas sur un seul
joueur :

| Titre | Revient à |
|---|---|
| Mécano | le plus de **composants** |
| Intendant | le plus de **nourriture** |
| Assidu | la plus longue **série** |
| Éclaireur | le plus de **défis** validés |

Purement honorifiques. Un opérateur peut en cumuler plusieurs ; en cas d'égalité, le titre n'est pas
décerné.

---

## La fusée

Au centre de la base, **dix états visuels indexés sur le nombre de gardiens vaincus**. Dix sur dix
donne le visuel maximal. Aucun effet sur les règles : c'est le trophée de la campagne.

---

## Récapitulatif des constantes

| Constante | Valeur |
|---|---|
| Dégâts totaux pour 1 habitant (croissance quotidienne) | 28 |
| Nourriture mangée par habitant et par jour | 0,008 |
| Perte quotidienne en cas de famine | 5 % de la part non nourrie, uniquement si la réserve est vide |
| Taille du gardien | référence × poids de la semaine × **0,78** × joueurs actifs |
| Taille du groupe | référence × poids de la semaine × 0,050 × joueurs actifs × progression |
| Progression des récompenses | +4 % par semaine de campagne, linéaire |
| Points d'un défi au classement | 1 par blessé ramené (référence × poids / 1000 × progression) |
| Composants pour atteindre 1 survivant | 14, dépensés seulement pour les blessés extraits |
| Nourriture pour installer 1 survivant | 12, dépensée seulement pour les blessés extraits |
| Réserve de nourriture protégée à l'extraction | 7 soirs de repas |
| Blessés des défis | pris dans le groupe de la semaine, partent les premiers, sans ressources ni avancement |
| Dégâts d'un défi au gardien | aucun |
| Pertes de base si le gardien survit | base × (1 − avancement)² × 35 % |
| Rescapés d'un défi | référence × poids du défi / 1000, par joueur qui le valide, versés le dimanche |
| Fréquence de synchronisation | 30 minutes |
| Fenêtre de calibration | 9 mois, réduits d'un mois à la fois tant qu'un joueur n'est pas couvert |
| Référence | moyenne des moyennes hebdomadaires par joueur actif, semaines vides à zéro, plancher 2 000 |
| Durée de la campagne | 10 semaines |

---

## Ce que la simulation vérifie

Les constantes ci-dessus ont été vérifiées le 04/09/2026 en faisant jouer des escouades simulées sur
dix semaines. À refaire avant tout réglage. Les invariants à conserver :

- Une escouade calibrée sur elle-même bat **8 gardiens sur 10**, les deux Élite manqués de peu.
- L'effort paie : assidu > régulier > mou, et le résultat par joueur est identique de 2 à 20 joueurs.
- À temps de jeu égal, aucun mode ne rapporte plus de 15 % qu'un autre.
- Une session d'un compétitif et deux à quatre Deathmatch est à l'équilibre entre composants et
  nourriture : ni l'un ni l'autre ne manque systématiquement.
- Une escouade qui ne joue que le week-end ne connaît pas la famine.
- Les défis valent environ 14 % de base en plus ; une semaine d'arrêt en coûte environ 27 %.

---

## Ce qui disparaît du système actuel

La nourriture en fenêtre glissante de sept jours, le multiplicateur de présence, le moral, l'efficacité
en habitants par unité de nourriture, l'échelle de paliers de base, la croissance asymptotique vers
un plafond mouvant, la calibration glissante du boss sur quatre semaines, les dégâts des défis au
boss et leur bonus d'équipe.
