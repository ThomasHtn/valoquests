# Les défis

Ce que le catalogue a le droit de demander, et comment un défi devient l'objectif qu'une semaine ou
une journée se joue. Les entrées elles-mêmes sont dans `CHALLENGES-CATALOGUE.md`, l'économie qui les
récompense dans `GAMEPLAY.md`.

---

## 1. Deux grilles écrites à la main

Chaque défi porte **deux séries de nombres** : une **référence**, pour une escouade qui joue
régulièrement, et une grille **experte**, pour une escouade qui valide la référence sans effort. Une
campagne fige son niveau à l'ouverture, au même moment que sa référence de calibration, et ne joue
que cette colonne.

Rien n'est calculé. C'est le point sur lequel le système précédent a échoué : les cibles étaient
dérivées de neuf mois d'historique par un facteur de volume borné et une ancre de talent par
statistique. Le catalogue devenait illisible — le même défi affichait douze kills à une escouade et
trente à une autre — et une escouade sous le plancher voyait chaque objectif quotidien divisé par
deux et demi, jusqu'à ce qu'un seul Deathmatch le règle. Le facteur de volume, les ancres de talent
et les coefficients de difficulté n'existent plus.

Ce que le niveau ne touche pas : la référence de calibration, qui continue de mesurer le volume de
jeu de l'escouade et de dimensionner les gardiens, les groupes et la valeur en points d'un défi.

**Le tirage copie, la ligne stocke, tout le reste lit.** Une sélection porte la grille du niveau au
moment où elle est tirée. Une campagne rejouée depuis son premier jour retrouve donc les objectifs
qu'elle a réellement joués, même si son niveau a changé depuis.

---

## 2. Ce que le catalogue ne peut pas mesurer

L'API Henrik renvoie **zéro headshot et zéro dégât** sur chaque Deathmatch et chaque Skirmish, sans
exception. Un défi qui les y mesure n'est pas difficile, il est impossible. Ces deux statistiques ne
sont donc lisibles qu'en Compétitif, en Non classé et en Team Deathmatch.

Trois interdits sont tenus par `ChallengeCatalogueCompatibilityTest`, sur le contenu des lignes et
jamais sur une liste de codes, pour qu'un défi ajouté plus tard tombe sous la même règle :

- aucune condition ne mesure les headshots ou les dégâts en Deathmatch ou en Skirmish ;
- aucun défi n'exige à la fois du Deathmatch et du Team Deathmatch, deux modes joués par à-coups et
  rarement dans la même semaine ;
- la grille experte n'est jamais inférieure à la référence, sur aucune condition, et déclare les
  mêmes conditions dans le même ordre.

---

## 3. Les cinq règles de contenu

### Règle 1 — Le Compétitif seul n'appartient qu'au palier le plus dur

VERY_HARD est la seule difficulté à filtrer sur `COMPETITIVE`. Partout ailleurs, y compris dans le
pool quotidien, le filtre est `COMPETITIVE_OR_UNRATED` ou un mode court. Un joueur qui ne touche
jamais au classé peut valider quatre des cinq défis hebdomadaires et tous les quotidiens.

### Règle 2 — Un défi ne se perd jamais d'un seul mauvais match

Les enchaînements et les taux tenus sur la semaine sont bannis : ils se perdent sans se rattraper.
La même question se pose en **N parties au-dessus d'une barre**, où rater une partie ne coûte que
cette partie. `ProgressMode.RATIO`, `MAX_STREAK` et `BASELINE` restent dans le code sans qu'aucune
entrée les déclare.

### Règle 3 — Trois agents au maximum

Dans les deux sens. Au-delà, les défis d'agents ne se distinguent plus que par un nombre.

### Règle 4 — Un quotidien se décide dans la journée

Un quotidien peut demander un volume de parties — le catalogue va jusqu'à six — mais une **barre à
franchir partie après partie reste dans deux parties**, sinon un seul mauvais match coûte la
journée.

### Règle 5 — Chaque échelle est continue

Une question posée à plusieurs paliers voit sa cible monter d'environ un quart à chaque échelon,
sans trou. Une escouade qui progresse retrouve le défi qu'elle suivait, plus haut, au lieu de le
perdre.

---

## 4. Comment un défi est tiré

**Cinq hebdomadaires le lundi**, un par difficulté, choisis pour couvrir des catégories différentes.
Un défi ne revient pas tant que sa difficulté n'a pas été parcourue ; c'est une préférence, pas une
contrainte, et une semaine qui ne peut être remplie qu'en réutilisant un défi l'est ainsi plutôt que
laissée incomplète.

**Un quotidien chaque matin**, commun à l'escouade, jamais un défi tiré dans les vingt-sept jours
précédents tant que le pool le permet, le moins récemment tiré sinon. Le pool compte vingt-huit
entrées, soit quatre semaines sans répétition.

Les deux tirages sont **déterministes** : le même jour ordonne toujours le pool de la même façon, si
bien qu'un redémarrage entre le tirage et son enregistrement ne peut pas donner deux défis au même
jour.

---

## 5. Ce qui décide de la valeur d'un défi

Le niveau ne rapporte rien de plus. Un défi vaut ce que `GAMEPLAY.md` lui donne selon sa cadence et
sa difficulté, multiplié par la référence de la campagne et par la progression de la semaine. Une
escouade experte joue donc des objectifs plus durs pour la même récompense : le niveau est une
question de justesse, pas de barème.

---

## 6. Où ça vit dans le code

- `SquadLevel` : les deux niveaux, et rien d'autre.
- `Challenge` : les deux grilles, et `conditionsFor(level)` qui choisit.
- `ChallengeCalibration` : ce contre quoi une semaine se joue — référence, index de semaine, niveau.
- `ChallengeSelectionFactory` : le seul endroit où une grille devient l'objectif d'une sélection.
- `DefaultWeeklyChallengeSelectionService` : les deux tirages et leurs fenêtres de non-répétition.
- `ChallengeGameMode` : les filtres de mode, Skirmish compris.
- `ChallengeCatalogueCompatibilityTest` : les règles ci-dessus, vérifiées sur la migration elle-même.
