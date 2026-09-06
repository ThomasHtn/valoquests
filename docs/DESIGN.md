# Direction de design

Ce que le frontend doit être, et les décisions qui en découlent. Complète `CLAUDE.md`, qui décrit
comment le code est organisé, pas à quoi il doit ressembler.

---

## 1. La consigne

Écrite par Thomas le 02/09/2026. C'est la source, tout le reste du document en découle.

- **Type** : « gamified app », « exploration », « ressource manager », "science fiction", "scan", "mission report"
- **Finition** : site à plusieurs milliers de dollars, proche d'une finition d'un site d'Apple et épuré sans texte superflux
- **Cible** : joueurs, coachs, e-sport.
- **Lecture** : le site doit pouvoir être lu et compris par quelqu'un qui n'est pas du milieu
  (recruteur, expert dev).
- **Icônes** : les utiliser et les répéter dans les phrases, les textes, pour bien montrer de quoi
  on parle.
- **Couleurs** : utiliser des couleurs qui vont avec le design de base.
- **Données** : jauges, graphiques et tableaux de bord, pour montrer le côté jeu de gestion.
- **Libs externes** : aucune restriction tant que c'est justifié. Le fait main n'est pas toujours la
  bonne solution.

---

## 2. Ce qui prime sur tout le reste

Le projet a déjà une identité, dite **« Expédition »**, définie dans `../frontend/src/styles`. Elle n'est pas à
réinventer. L'ordre de priorité est toujours : les mots de Thomas, puis le système existant, puis mes
choix.

| | Valeur | Où |
|---|---|---|
| Marque | `#d9954a` ambre, avec `#e8ab6b` en variante claire | `colors.css` |
| Sol | `#0f1c26` teal-navy, `#0a151d` en creusé | `colors.css` |
| Panneaux | `#1b2c3a`, `#253645`, `#33495b` | `colors.css` |
| Bords | `--color-edge` et `--color-edge-strong`, translucides | `colors.css` |
| Texte | `#ece8e1`, `#a4a7a6`, `#868b8d` | `colors.css` |
| Display | Oswald 400–700, auto-hébergée | `typography.css` |
| Texte courant | Barlow Condensed 400/500/600/700, auto-hébergée | `typography.css` |
| Icônes | `@lucide/angular` | `../frontend/package.json` |

**Le site est sombre, et seulement sombre.** Il n'y a pas de thème clair et il n'y en aura pas. C'est
un choix de direction, pas un oubli.

Avant d'écrire une suite de classes à la main, chercher l'utilitaire qui existe déjà : `notch-tr`,
`clip-hex`, `label-caption`, `menu-panel`, `menu-option`, `ambient-field`. Ils existent parce que la
version écrite à la main avait déjà divergé d'un écran à l'autre.

---

## 4. Les icônes dans le texte

Une icône n'est pas une décoration en début de ligne, c'est un mot. Elle apparaît **là où le mot
apparaît**, y compris au milieu d'une phrase.

- Chaque chiffre est accompagné de l'icône de ce qu'il compte, à chaque fois qu'il apparaît :
  habitants, scrap, vivres, points de vie, défis, jours.
- Les mêmes icônes se répètent d'un écran à l'autre pour la même chose. Une clé à molette veut dire
  scrap partout, jamais « réglages ».
- Toutes viennent de Lucide. Pas de deuxième jeu, pas d'emoji.

---

## 5. Lisible par quelqu'un qui ne joue pas

C'est la contrainte la plus exigeante du brief, et celle qu'on rate en premier.

- **Aucun jargon Valorant sans traduction.** « ACS » s'écrit « score de combat par round », « ADR »
  s'écrit « dégâts par round ». Le sigle peut suivre entre parenthèses, jamais précéder. L'ideal est la presence de bulle info au hover pour explique ce que cela veut dire et à quoi ça correspond et comment c'est calculé
- **Aucun terme de mode approximatif.** On écrit « en Compétitif ou Non classé », jamais « en partie
  longue » ni « en ranked ». Voir `CHALLENGES.md`.
- **Chaque jauge dit ce qui se passe quand elle se remplit.** Une barre sans conséquence énoncée est
  une décoration.

Le test : un recruteur qui ouvre la page comprend en trente secondes ce que fait l'escouade et si
elle s'en sort bien.

---

## 7. Les libs externes

**La règle** : une lib entre quand elle règle un problème réellement difficile — placement,
physique, projection de données, accessibilité. Elle n'entre pas pour remplacer vingt lignes de CSS.
Chaque ajout se justifie dans le message de commit.

**Déjà en place, à garder**

| Lib | Ce qu'elle fait |
|---|---|
| `@lucide/angular` | le jeu d'icônes, source unique |
| `chart.js` | les vrais graphiques multi-séries, chargé en lazy sur `/players/:id` seulement |
| _(aucune pour les jauges radiales)_ | `shared/progress-circle` dessine l'anneau lui-même : deux cercles SVG et un `stroke-dashoffset`, la bibliothèque `angular-svg-round-progressbar` a été retirée le 05/09/2026 |

**À ajouter, justifié**

| Lib | Poids | Pourquoi |
|---|---|---|
| `@floating-ui/dom` | ~10 kB | placement des infobulles avec détection de collision et retournement. Le `shared/tooltip` fait main ne sait pas gérer un bord d'écran, et un tableau de bord dense en a partout. |
| `motion` | ~18 kB | ressorts et transitions de position. Les animations Angular font du timing, pas de la physique — c'est précisément l'écart entre une interface correcte et une finition Apple. |

**Où le fait main reste meilleur**

- **Les sparklines.** Un `<path>` SVG calculé en dix lignes pèse moins qu'un import de Chart.js et se
  contrôle mieux. Chart.js garde les vraies séries.
- **La scène de la ville.** C'est un dessin propre au projet. Aucune lib ne le fait, et c'est
  l'élément d'identité le plus fort du site.
- **Les états vides.** `shared/empty-plate` dessine ses quatre illustrations dans l'idiome de la
  scène (trait plein = ce qui existe, pointillé = ce qui reste à faire). Un pack d'illustrations en
  aplats jurerait sur ce fond ; choix validé par Thomas le 05/09/2026.
- **Le compteur qui s'incrémente.** `appCountUp` existe, fonctionne, et ne coûte rien.

---

## 8. Contraintes acquises, à ne pas violer

Chacune vient d'une correction déjà faite. Les réintroduire serait refaire une erreur connue.

- **Pas d'ombre ni de dégradé sur les listes escouade et profil.** Fond plat d'origine, libellé porté
  par la cellule, pas d'en-tête collant.
- **Le codage couleur des lignes de tableau est intouchable.** Densifier oui, aplatir toutes les
  lignes à une couleur uniforme non. Attention au piège : `panel` et `box-shadow` ne donnent pas le
  même résultat sur une ligne teintée.
- **Oswald en capitales avec accents français** : jamais d'interlignage sous 1, sinon les accents des
  majuscules se font couper. Et on mesure les largeurs en `rem`, pas en `ch` — la fonte est condensée,
  le `ch` ment.
- **Pas d'illustration faite main ni de lib externe en douce.** Les deux sont autorisées, mais le
  choix revient à Thomas : le proposer, pas le décider.

---

## 9. Comment on travaille

Toute refonte d'écran passe par une **maquette publiée en artifact** avant la moindre ligne
d'Angular. Pas de nouveau signal, pas de nouveau modèle, pas de service touché tant que la direction
n'est pas validée à l'écran.

Pour que la maquette soit fidèle, les vraies polices sont embarquées en `data:` URI depuis
`../frontend/public/fonts` — le CSP des artifacts bloque tout hôte externe, et une fonte de secours fausse
complètement le rendu. Les couleurs viennent de `src/styles/colors.css`, jamais inventées.

Quand plusieurs directions sont demandées sur un même écran, elles vont dans **une seule maquette** à
onglets CSS, pas dans plusieurs URL : on compare en basculant un onglet, pas en jonglant entre
fenêtres.

Essaye d'etre fidele à l'application durant le maquettage, souvent le resultat differe de ce qui est affiché ou de l'etat reel de l'application
