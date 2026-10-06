# Catalogue des défis

128 défis : **28 quotidiens** et **100 hebdomadaires**, vingt par difficulté. Chaque défi porte deux
séries de nombres, écrites à la main, jamais calculées. Les règles qui gouvernent ces entrées sont
dans `CHALLENGES.md`, l'économie dans `GAMEPLAY.md`.

## Comment lire les tableaux

**Réf.** est ce qu'une escouade jouant régulièrement doit atteindre, **Expert** ce qu'une escouade
qui valide la référence sans effort doit atteindre. Une campagne fige son niveau à l'ouverture et ne
joue que cette colonne. Un défi qui compte plusieurs parties avec une barre par partie s'écrit
`parties × barre` ; un défi qui combine deux conditions les sépare par un `+`.

## Ce que le catalogue ne peut pas mesurer

L'API Henrik renvoie **zéro headshot et zéro dégât** sur chaque Deathmatch et chaque Skirmish. Ces
deux statistiques n'existent que sur les modes joués en rounds — Compétitif, Non classé et Team
Deathmatch. Aucun défi ne les mesure ailleurs, et un test du catalogue refuse toute entrée qui
essaierait.

Deux autres interdits, tenus par le même test : aucun défi n'exige à la fois du Deathmatch et du
Team Deathmatch, deux modes joués par à-coups et rarement la même semaine ; et la grille experte
n'est jamais inférieure à la référence, sur aucune condition.

---

## Quotidiens — 28 défis

Un seul est tiré par jour, commun à l'escouade, résolu le soir même. Aucun n'exige du Compétitif seul. Vingt-huit entrées donnent quatre semaines sans répétition.

| Code                       | Nom                         | Défi                                                                                           | Cat.        | Mode                     | Réf.   | Expert |
| -------------------------- | --------------------------- | ---------------------------------------------------------------------------------------------- | ----------- | ------------------------ | ------ | ------ |
| `DAILY_ONE_LONG`           | Match sérieux               | Jouer 1 match en Compétitif ou Non classé dans la journée                                      | MATCHES     | Compétitif ou Non classé | 1      | 2      |
| `DAILY_TWO_MATCHES`        | Session du jour             | Jouer 3 matchs, tous modes confondus, dans la journée                                          | MATCHES     | Tous modes               | 3      | 6      |
| `DAILY_LONG_ROUNDS`        | Rounds du jour              | Jouer 20 rounds en Compétitif ou Non classé dans la journée                                    | MATCHES     | Compétitif ou Non classé | 20     | 40     |
| `DAILY_DAY_KILLS`          | Récolte du jour             | Cumuler 50 kills, tous modes confondus, dans la journée                                        | PERFORMANCE | Tous modes               | 50     | 100    |
| `DAILY_LONG_KILLS_DAY`     | Chasse du jour              | Cumuler 20 kills en Compétitif ou Non classé dans la journée                                   | PERFORMANCE | Compétitif ou Non classé | 20     | 35     |
| `DAILY_DM_KILLS_DAY`       | Chasse en Deathmatch        | Cumuler 40 kills en Deathmatch dans la journée                                                 | PERFORMANCE | Deathmatch               | 40     | 70     |
| `DAILY_SKIRMISH_KILLS_DAY` | Chasse en Skirmish          | Cumuler 20 kills en Skirmish 2v2 dans la journée                                               | PERFORMANCE | Skirmish 2v2             | 20     | 35     |
| `DAILY_DAY_SCORE`          | Score du jour               | Cumuler 12 000 de score de combat, tous modes confondus, dans la journée                       | PERFORMANCE | Tous modes               | 12 000 | 25 000 |
| `DAILY_LONG_DOUBLE`        | Doublé sérieux              | Terminer 2 matchs en Compétitif ou Non classé dans la journée avec au moins 15 kills par match | PERFORMANCE | Compétitif ou Non classé | 2 × 15 | 2 × 20 |
| `DAILY_LONG_HEADSHOTS`     | Têtes du jour               | Terminer 1 match en Compétitif ou Non classé dans la journée avec au moins 6 headshots         | AIM         | Compétitif ou Non classé | 6      | 15     |
| `DAILY_DAY_HEADSHOTS`      | Moisson de têtes            | Cumuler 25 headshots, tous modes confondus, dans la journée                                    | AIM         | Tous modes               | 25     | 40     |
| `DAILY_DAY_ASSISTS`        | Appui du jour               | Cumuler 5 assists, tous modes confondus, dans la journée                                       | SUPPORT     | Tous modes               | 5      | 10     |
| `DAILY_DAY_DAMAGE`         | Dégâts du jour              | Cumuler 5 000 dégâts, tous modes confondus, dans la journée                                    | DAMAGE      | Tous modes               | 5 000  | 10 000 |
| `DAILY_LONG_DAMAGE`        | Pression du jour            | Cumuler 3 000 dégâts en Compétitif ou Non classé dans la journée                               | DAMAGE      | Compétitif ou Non classé | 3 000  | 6 000  |
| `DAILY_TDM_DAMAGE`         | Dégâts en Team Deathmatch   | Cumuler 4 000 dégâts en Team Deathmatch dans la journée                                        | DAMAGE      | Team Deathmatch          | 4 000  | 8 000  |
| `DAILY_DM_ROUTINE`         | Routine Deathmatch          | Jouer 2 matchs en Deathmatch dans la journée                                                   | TRAINING    | Deathmatch               | 2      | 4      |
| `DAILY_SKIRMISH_ROUTINE`   | Routine Skirmish            | Jouer 2 matchs en Skirmish 2v2 dans la journée                                                 | TRAINING    | Skirmish 2v2             | 2      | 4      |
| `DAILY_DM_KILLS`           | Deathmatch du jour          | Terminer 1 match en Deathmatch dans la journée avec au moins 20 kills                          | TRAINING    | Deathmatch               | 20     | 30     |
| `DAILY_DM_DOUBLE`          | Double Deathmatch           | Terminer 2 matchs en Deathmatch dans la journée avec au moins 15 kills par match               | TRAINING    | Deathmatch               | 2 × 15 | 2 × 20 |
| `DAILY_TDM_KILLS`          | Team Deathmatch du jour     | Terminer 1 match en Team Deathmatch dans la journée avec au moins 25 kills                     | TRAINING    | Team Deathmatch          | 25     | 32     |
| `DAILY_LONG_WIN`           | Victoire du jour            | Remporter 1 match en Compétitif ou Non classé dans la journée                                  | VICTORY     | Compétitif ou Non classé | 1      | 2      |
| `DAILY_TWO_WINS`           | Victoires du jour           | Remporter 2 matchs, tous modes confondus, dans la journée                                      | VICTORY     | Tous modes               | 2      | 4      |
| `DAILY_TDM_WIN`            | Victoire en Team Deathmatch | Remporter 1 match en Team Deathmatch dans la journée                                           | VICTORY     | Team Deathmatch          | 1      | 2      |
| `DAILY_SKIRMISH_WIN`       | Escarmouche gagnée          | Remporter 1 match en Skirmish 2v2 dans la journée                                              | VICTORY     | Skirmish 2v2             | 1      | 2      |
| `DAILY_MODE_WINS`          | Victoires variées           | Remporter au moins un match dans 2 modes différents dans la journée                            | VICTORY     | Tous modes               | 2      | 3      |
| `DAILY_AGENT_VARIETY`      | Roulement d'agents          | Jouer au moins un match avec 2 agents différents, tous modes confondus, dans la journée        | AGENT       | Tous modes               | 2      | 3      |
| `DAILY_AGENT_WINS`         | Victoires d'agents          | Remporter au moins un match avec 2 agents différents, tous modes confondus, dans la journée    | AGENT       | Tous modes               | 2      | 3      |
| `DAILY_TWO_MODES`          | Trois modes                 | Jouer au moins un match dans 3 modes différents dans la journée                                | VARIETY     | Tous modes               | 3      | 3      |

Répartition : MATCHES 3, PERFORMANCE 6, AIM 2, SUPPORT 1, DAMAGE 3, TRAINING 5, VICTORY 5, AGENT 2, VARIETY 1.

---

## EASY — 20 défis

| Code                   | Nom                     | Défi                                                                                               | Cat.        | Mode                     | Réf.   | Expert   |
| ---------------------- | ----------------------- | -------------------------------------------------------------------------------------------------- | ----------- | ------------------------ | ------ | -------- |
| `EASY_LONG_MATCHES`    | Mise en jambes          | Jouer 3 matchs en Compétitif ou Non classé sur la semaine                                          | MATCHES     | Compétitif ou Non classé | 3      | 5        |
| `EASY_ANY_MATCHES`     | Présent au rapport      | Jouer 15 matchs, tous modes confondus, sur la semaine                                              | MATCHES     | Tous modes               | 15     | 20       |
| `EASY_LONG_ROUNDS`     | Premiers rounds         | Jouer 60 rounds en Compétitif ou Non classé sur la semaine                                         | MATCHES     | Compétitif ou Non classé | 60     | 90       |
| `EASY_LONG_KILLS`      | Tableau de chasse       | Cumuler 100 kills en Compétitif ou Non classé sur la semaine                                       | PERFORMANCE | Compétitif ou Non classé | 100    | 150      |
| `EASY_LONG_SCORE`      | Score d'entrée          | Cumuler 25 000 de score de combat en Compétitif ou Non classé sur la semaine                       | PERFORMANCE | Compétitif ou Non classé | 25 000 | 50 000   |
| `EASY_LONG_KILL_GAMES` | Bonne sortie            | Terminer 3 matchs en Compétitif ou Non classé sur la semaine avec au moins 13 kills par match      | PERFORMANCE | Compétitif ou Non classé | 3 × 13 | 3 × 16   |
| `EASY_LONG_HEADSHOTS`  | Dans la tête            | Cumuler 40 headshots en Compétitif ou Non classé sur la semaine                                    | AIM         | Compétitif ou Non classé | 40     | 60       |
| `EASY_LONG_HS_GAMES`   | Visée appliquée         | Terminer 3 matchs en Compétitif ou Non classé sur la semaine avec au moins 8 headshots par match   | AIM         | Compétitif ou Non classé | 3 × 8  | 3 × 12   |
| `EASY_SKIRMISH_KILLS`  | Duelliste               | Cumuler 40 kills en Skirmish 2v2 sur la semaine                                                    | TRAINING    | Skirmish 2v2             | 40     | 60       |
| `EASY_LONG_ASSISTS`    | Coup de main            | Cumuler 30 assists en Compétitif ou Non classé sur la semaine                                      | SUPPORT     | Compétitif ou Non classé | 30     | 40       |
| `EASY_LONG_DAMAGE`     | Premiers dégâts         | Cumuler 13 000 dégâts en Compétitif ou Non classé sur la semaine                                   | DAMAGE      | Compétitif ou Non classé | 13 000 | 20 000   |
| `EASY_DM_ROUTINE`      | Routine d'échauffement  | Jouer 7 matchs en Deathmatch sur la semaine                                                        | TRAINING    | Deathmatch               | 7      | 10       |
| `EASY_TDM_ROUTINE`     | Routine Team Deathmatch | Jouer 3 matchs en Team Deathmatch sur la semaine                                                   | TRAINING    | Team Deathmatch          | 3      | 5        |
| `EASY_DM_KILLS`        | Chair à canon           | Cumuler 150 kills en Deathmatch sur la semaine                                                     | TRAINING    | Deathmatch               | 150    | 200      |
| `EASY_LONG_WINS`       | Doublé                  | Remporter 2 matchs en Compétitif ou Non classé sur la semaine                                      | VICTORY     | Compétitif ou Non classé | 2      | 3        |
| `EASY_TDM_WINS`        | Échauffement gagnant    | Remporter 2 matchs en Team Deathmatch sur la semaine                                               | VICTORY     | Team Deathmatch          | 2      | 3        |
| `EASY_PLAY_DAYS`       | Présence régulière      | Jouer au moins un match, tous modes confondus, sur 3 jours différents de la semaine                | CONSISTENCY | Tous modes               | 3      | 4        |
| `EASY_LONG_KD_GAMES`   | Bilan positif           | Terminer 3 matchs en Compétitif ou Non classé sur la semaine avec un K/D d'au moins 1,00 par match | CONSISTENCY | Compétitif ou Non classé | 3 × 1  | 3 × 1,10 |
| `EASY_AGENT_VARIETY`   | Polyvalent              | Jouer au moins un match avec 2 agents différents en Compétitif ou Non classé sur la semaine        | AGENT       | Compétitif ou Non classé | 2      | 3        |
| `EASY_DAY_BEST_KILLS`  | Grosse journée          | Cumuler 60 kills, tous modes confondus, dans une même journée de la semaine                        | VARIETY     | Tous modes               | 60     | 80       |

Répartition : MATCHES 3, PERFORMANCE 3, AIM 2, SUPPORT 1, DAMAGE 1, TRAINING 4, VICTORY 2, CONSISTENCY 2, AGENT 1, VARIETY 1.

---

## NORMAL — 20 défis

| Code                     | Nom                 | Défi                                                                                               | Cat.        | Mode                     | Réf.     | Expert   |
| ------------------------ | ------------------- | -------------------------------------------------------------------------------------------------- | ----------- | ------------------------ | -------- | -------- |
| `NORMAL_LONG_MATCHES`    | Rythme de croisière | Jouer 4 matchs en Compétitif ou Non classé sur la semaine                                          | MATCHES     | Compétitif ou Non classé | 4        | 6        |
| `NORMAL_ANY_MATCHES`     | Toujours là         | Jouer 18 matchs, tous modes confondus, sur la semaine                                              | MATCHES     | Tous modes               | 18       | 25       |
| `NORMAL_LONG_ROUNDS`     | Rounds au compteur  | Jouer 75 rounds en Compétitif ou Non classé sur la semaine                                         | MATCHES     | Compétitif ou Non classé | 75       | 110      |
| `NORMAL_LONG_KILLS`      | Chasseur            | Cumuler 125 kills en Compétitif ou Non classé sur la semaine                                       | PERFORMANCE | Compétitif ou Non classé | 125      | 190      |
| `NORMAL_LONG_SCORE`      | Score solide        | Cumuler 30 000 de score de combat en Compétitif ou Non classé sur la semaine                       | PERFORMANCE | Compétitif ou Non classé | 30 000   | 60 000   |
| `NORMAL_LONG_KILL_GAMES` | Sorties régulières  | Terminer 4 matchs en Compétitif ou Non classé sur la semaine avec au moins 14 kills par match      | PERFORMANCE | Compétitif ou Non classé | 4 × 14   | 4 × 17   |
| `NORMAL_LONG_HEADSHOTS`  | Viseur réglé        | Cumuler 50 headshots en Compétitif ou Non classé sur la semaine                                    | AIM         | Compétitif ou Non classé | 50       | 75       |
| `NORMAL_LONG_HS_GAMES`   | Viseur constant     | Terminer 4 matchs en Compétitif ou Non classé sur la semaine avec au moins 10 headshots par match  | AIM         | Compétitif ou Non classé | 4 × 10   | 4 × 14   |
| `NORMAL_LONG_ASSISTS`    | Soutien fiable      | Cumuler 38 assists en Compétitif ou Non classé sur la semaine                                      | SUPPORT     | Compétitif ou Non classé | 38       | 50       |
| `NORMAL_LONG_DAMAGE`     | Pression            | Cumuler 16 000 dégâts en Compétitif ou Non classé sur la semaine                                   | DAMAGE      | Compétitif ou Non classé | 16 000   | 25 000   |
| `NORMAL_LONG_ADR_GAMES`  | Pression constante  | Terminer 4 matchs en Compétitif ou Non classé sur la semaine avec au moins 130 d'ADR par match     | DAMAGE      | Compétitif ou Non classé | 4 × 130  | 4 × 155  |
| `NORMAL_DM_KILLS`        | Machine à frags     | Cumuler 190 kills en Deathmatch sur la semaine                                                     | TRAINING    | Deathmatch               | 190      | 250      |
| `NORMAL_TDM_KILLS`       | Mêlée générale      | Cumuler 120 kills en Team Deathmatch sur la semaine                                                | TRAINING    | Team Deathmatch          | 120      | 160      |
| `NORMAL_DM_KILL_GAMES`   | Deathmatch appliqué | Terminer 6 matchs en Deathmatch sur la semaine avec au moins 20 kills par match                    | TRAINING    | Deathmatch               | 6 × 20   | 8 × 25   |
| `NORMAL_LONG_WINS`       | Enchaînement        | Remporter 3 matchs en Compétitif ou Non classé sur la semaine                                      | VICTORY     | Compétitif ou Non classé | 3        | 4        |
| `NORMAL_DAY_WINS`        | Journée gagnante    | Remporter 3 matchs, tous modes confondus, dans une même journée de la semaine                      | VICTORY     | Tous modes               | 3        | 4        |
| `NORMAL_PLAY_DAYS`       | Assidu              | Jouer au moins un match, tous modes confondus, sur 4 jours différents de la semaine                | CONSISTENCY | Tous modes               | 4        | 5        |
| `NORMAL_LONG_KD_GAMES`   | À l'équilibre       | Terminer 4 matchs en Compétitif ou Non classé sur la semaine avec un K/D d'au moins 1,05 par match | CONSISTENCY | Compétitif ou Non classé | 4 × 1,05 | 4 × 1,15 |
| `NORMAL_MODE_VARIETY`    | Touche-à-tout       | Jouer au moins un match dans 4 modes différents sur la semaine                                     | VARIETY     | Tous modes               | 4        | 4        |
| `NORMAL_DAY_BEST_KILLS`  | Journée chargée     | Cumuler 75 kills, tous modes confondus, dans une même journée de la semaine                        | VARIETY     | Tous modes               | 75       | 100      |

Répartition : MATCHES 3, PERFORMANCE 3, AIM 2, SUPPORT 1, DAMAGE 2, TRAINING 3, VICTORY 2, CONSISTENCY 2, VARIETY 2.

---

## MEDIUM — 20 défis

| Code                     | Nom                          | Défi                                                                                               | Cat.        | Mode                     | Réf.     | Expert   |
| ------------------------ | ---------------------------- | -------------------------------------------------------------------------------------------------- | ----------- | ------------------------ | -------- | -------- |
| `MEDIUM_LONG_MATCHES`    | Semaine pleine               | Jouer 5 matchs en Compétitif ou Non classé sur la semaine                                          | MATCHES     | Compétitif ou Non classé | 5        | 8        |
| `MEDIUM_ANY_MATCHES`     | Marathonien                  | Jouer 22 matchs, tous modes confondus, sur la semaine                                              | MATCHES     | Tous modes               | 22       | 30       |
| `MEDIUM_LONG_ROUNDS`     | Rounds à la pelle            | Jouer 95 rounds en Compétitif ou Non classé sur la semaine                                         | MATCHES     | Compétitif ou Non classé | 95       | 140      |
| `MEDIUM_LONG_KILLS`      | Prédateur                    | Cumuler 155 kills en Compétitif ou Non classé sur la semaine                                       | PERFORMANCE | Compétitif ou Non classé | 155      | 240      |
| `MEDIUM_LONG_SCORE`      | Score massif                 | Cumuler 38 000 de score de combat en Compétitif ou Non classé sur la semaine                       | PERFORMANCE | Compétitif ou Non classé | 38 000   | 75 000   |
| `MEDIUM_LONG_KILL_GAMES` | Grosses sorties              | Terminer 5 matchs en Compétitif ou Non classé sur la semaine avec au moins 15 kills par match      | PERFORMANCE | Compétitif ou Non classé | 5 × 15   | 5 × 18   |
| `MEDIUM_LONG_ACS_GAMES`  | Impact                       | Terminer 5 matchs en Compétitif ou Non classé sur la semaine avec au moins 225 d'ACS par match     | PERFORMANCE | Compétitif ou Non classé | 5 × 225  | 5 × 245  |
| `MEDIUM_LONG_HS_GAMES`   | Précision répétée            | Terminer 5 matchs en Compétitif ou Non classé sur la semaine avec au moins 11 headshots par match  | AIM         | Compétitif ou Non classé | 5 × 11   | 5 × 15   |
| `MEDIUM_LONG_HEADSHOTS`  | Tir groupé                   | Cumuler 65 headshots en Compétitif ou Non classé sur la semaine                                    | AIM         | Compétitif ou Non classé | 65       | 95       |
| `MEDIUM_LONG_ASSISTS`    | Pilier d'équipe              | Cumuler 48 assists en Compétitif ou Non classé sur la semaine                                      | SUPPORT     | Compétitif ou Non classé | 48       | 62       |
| `MEDIUM_LONG_DAMAGE`     | Artillerie                   | Cumuler 20 000 dégâts en Compétitif ou Non classé sur la semaine                                   | DAMAGE      | Compétitif ou Non classé | 20 000   | 31 000   |
| `MEDIUM_LONG_ADR_GAMES`  | Pression soutenue            | Terminer 5 matchs en Compétitif ou Non classé sur la semaine avec au moins 135 d'ADR par match     | DAMAGE      | Compétitif ou Non classé | 5 × 135  | 5 × 160  |
| `MEDIUM_DM_MATCHES`      | Salle d'entraînement         | Jouer 11 matchs en Deathmatch sur la semaine                                                       | TRAINING    | Deathmatch               | 11       | 16       |
| `MEDIUM_SKIRMISH_KILLS`  | Duelliste confirmé           | Cumuler 100 kills en Skirmish 2v2 sur la semaine                                                   | TRAINING    | Skirmish 2v2             | 100      | 150      |
| `MEDIUM_DAY_BEST_KILLS`  | Journée intense              | Cumuler 95 kills, tous modes confondus, dans une même journée de la semaine                        | VARIETY     | Tous modes               | 95       | 130      |
| `MEDIUM_TDM_WINS`        | Victoires en Team Deathmatch | Remporter 3 matchs en Team Deathmatch sur la semaine                                               | VICTORY     | Team Deathmatch          | 3        | 5        |
| `MEDIUM_WIN_DAYS`        | Jours de victoire            | Remporter au moins un match en Compétitif ou Non classé sur 2 jours différents de la semaine       | VICTORY     | Compétitif ou Non classé | 2        | 3        |
| `MEDIUM_PLAY_DAYS`       | Fidèle au poste              | Jouer au moins un match, tous modes confondus, sur 5 jours différents de la semaine                | CONSISTENCY | Tous modes               | 5        | 6        |
| `MEDIUM_LONG_KD_GAMES`   | Au-dessus de la barre        | Terminer 5 matchs en Compétitif ou Non classé sur la semaine avec un K/D d'au moins 1,10 par match | CONSISTENCY | Compétitif ou Non classé | 5 × 1,10 | 5 × 1,20 |
| `MEDIUM_AGENT_WINS`      | Vainqueur polyvalent         | Remporter au moins un match avec 3 agents différents en Compétitif ou Non classé sur la semaine    | AGENT       | Compétitif ou Non classé | 3        | 3        |

Répartition : MATCHES 3, PERFORMANCE 4, AIM 2, SUPPORT 1, DAMAGE 2, TRAINING 2, VICTORY 2, CONSISTENCY 2, AGENT 1, VARIETY 1.

---

## HARD — 20 défis

| Code                   | Nom                    | Défi                                                                                               | Cat.        | Mode                     | Réf.     | Expert   |
| ---------------------- | ---------------------- | -------------------------------------------------------------------------------------------------- | ----------- | ------------------------ | -------- | -------- |
| `HARD_ANY_MATCHES`     | Insatiable             | Jouer 28 matchs, tous modes confondus, sur la semaine                                              | MATCHES     | Tous modes               | 28       | 38       |
| `HARD_LONG_ROUNDS`     | Rounds sans fin        | Jouer 145 rounds en Compétitif ou Non classé sur la semaine                                        | MATCHES     | Compétitif ou Non classé | 145      | 210      |
| `HARD_LONG_MATCHES`    | Semaine chargée        | Jouer 9 matchs en Compétitif ou Non classé sur la semaine                                          | MATCHES     | Compétitif ou Non classé | 9        | 12       |
| `HARD_LONG_KILLS`      | Centurion              | Cumuler 195 kills en Compétitif ou Non classé sur la semaine                                       | PERFORMANCE | Compétitif ou Non classé | 195      | 300      |
| `HARD_LONG_SCORE`      | Score écrasant         | Cumuler 48 000 de score de combat en Compétitif ou Non classé sur la semaine                       | PERFORMANCE | Compétitif ou Non classé | 48 000   | 95 000   |
| `HARD_LONG_KILL_GAMES` | Carnage régulier       | Terminer 6 matchs en Compétitif ou Non classé sur la semaine avec au moins 16 kills par match      | PERFORMANCE | Compétitif ou Non classé | 6 × 16   | 6 × 19   |
| `HARD_LONG_ACS_GAMES`  | Impact soutenu         | Terminer 6 matchs en Compétitif ou Non classé sur la semaine avec au moins 250 d'ACS par match     | PERFORMANCE | Compétitif ou Non classé | 6 × 250  | 6 × 270  |
| `HARD_DAY_BEST_KILLS`  | Journée sanglante      | Cumuler 115 kills, tous modes confondus, dans une même journée de la semaine                       | PERFORMANCE | Tous modes               | 115      | 155      |
| `HARD_LONG_HEADSHOTS`  | Tireur d'élite         | Cumuler 80 headshots en Compétitif ou Non classé sur la semaine                                    | AIM         | Compétitif ou Non classé | 80       | 120      |
| `HARD_LONG_HS_GAMES`   | Précision chirurgicale | Terminer 6 matchs en Compétitif ou Non classé sur la semaine avec au moins 12 headshots par match  | AIM         | Compétitif ou Non classé | 6 × 12   | 6 × 16   |
| `HARD_LONG_ASSISTS`    | Colonne vertébrale     | Cumuler 60 assists en Compétitif ou Non classé sur la semaine                                      | SUPPORT     | Compétitif ou Non classé | 60       | 78       |
| `HARD_LONG_DAMAGE`     | Bombardement           | Cumuler 25 000 dégâts en Compétitif ou Non classé sur la semaine                                   | DAMAGE      | Compétitif ou Non classé | 25 000   | 39 000   |
| `HARD_LONG_ADR_GAMES`  | Rouleau compresseur    | Terminer 6 matchs en Compétitif ou Non classé sur la semaine avec au moins 140 d'ADR par match     | DAMAGE      | Compétitif ou Non classé | 6 × 140  | 6 × 165  |
| `HARD_DM_KILL_GAMES`   | Deathmatch dominé      | Terminer 10 matchs en Deathmatch sur la semaine avec au moins 25 kills par match                   | TRAINING    | Deathmatch               | 10 × 25  | 12 × 30  |
| `HARD_TDM_KILL_GAMES`  | Team Deathmatch dominé | Terminer 3 matchs en Team Deathmatch sur la semaine avec au moins 28 kills par match               | TRAINING    | Team Deathmatch          | 3 × 28   | 4 × 30   |
| `HARD_LONG_WINS`       | Série gagnante         | Remporter 5 matchs en Compétitif ou Non classé sur la semaine                                      | VICTORY     | Compétitif ou Non classé | 5        | 8        |
| `HARD_WIN_DAYS`        | Semaine victorieuse    | Remporter au moins un match en Compétitif ou Non classé sur 3 jours différents de la semaine       | VICTORY     | Compétitif ou Non classé | 3        | 4        |
| `HARD_DAILY_PLAYER`    | Sur tous les fronts    | Jouer au moins un match, tous modes confondus, sur 6 jours différents de la semaine                | CONSISTENCY | Tous modes               | 6        | 7        |
| `HARD_LONG_KD_GAMES`   | Dominant               | Terminer 6 matchs en Compétitif ou Non classé sur la semaine avec un K/D d'au moins 1,15 par match | CONSISTENCY | Compétitif ou Non classé | 6 × 1,15 | 6 × 1,25 |
| `HARD_MODE_VARIETY`    | Éclectique             | Jouer au moins un match dans 5 modes différents sur la semaine                                     | VARIETY     | Tous modes               | 5        | 5        |

Répartition : MATCHES 3, PERFORMANCE 5, AIM 2, SUPPORT 1, DAMAGE 2, TRAINING 2, VICTORY 2, CONSISTENCY 2, VARIETY 1.

---

## VERY_HARD — 20 défis

Seul palier exigeant le **Compétitif seul**, sur dix-huit de ses vingt entrées.

| Code                         | Nom                   | Défi                                                                                            | Cat.        | Mode       | Réf.      | Expert    |
| ---------------------------- | --------------------- | ----------------------------------------------------------------------------------------------- | ----------- | ---------- | --------- | --------- |
| `VERY_HARD_COMP_ROUNDS`      | Rounds classés        | Jouer 180 rounds en Compétitif sur la semaine                                                   | MATCHES     | Compétitif | 180       | 260       |
| `VERY_HARD_DAY_COMP_MATCHES` | Journée classée       | Jouer 4 matchs en Compétitif dans une même journée de la semaine                                | MATCHES     | Compétitif | 4         | 4         |
| `VERY_HARD_COMP_KILLS`       | Exécuteur             | Cumuler 245 kills en Compétitif sur la semaine                                                  | PERFORMANCE | Compétitif | 245       | 375       |
| `VERY_HARD_COMP_SCORE`       | Score de légende      | Cumuler 60 000 de score de combat en Compétitif sur la semaine                                  | PERFORMANCE | Compétitif | 60 000    | 120 000   |
| `VERY_HARD_COMP_KILL_GAMES`  | Carnage répété        | Terminer 7 matchs en Compétitif sur la semaine avec au moins 17 kills par match                 | PERFORMANCE | Compétitif | 7 × 17    | 7 × 20    |
| `VERY_HARD_COMP_ACS_GAMES`   | Niveau tenu           | Terminer 7 matchs en Compétitif sur la semaine avec au moins 275 d'ACS par match                | PERFORMANCE | Compétitif | 7 × 275   | 7 × 295   |
| `VERY_HARD_COMP_KD_GAMES`    | Intouchable           | Terminer 7 matchs en Compétitif sur la semaine avec un K/D d'au moins 1,20 par match            | PERFORMANCE | Compétitif | 7 × 1,20  | 7 × 1,30  |
| `VERY_HARD_DAY_BEST_KILLS`   | Journée de gala       | Cumuler 60 kills en Compétitif dans une même journée de la semaine                              | PERFORMANCE | Compétitif | 60        | 75        |
| `VERY_HARD_COMP_HEADSHOTS`   | Une balle, une tête   | Cumuler 100 headshots en Compétitif sur la semaine                                              | AIM         | Compétitif | 100       | 150       |
| `VERY_HARD_COMP_HS_GAMES`    | Précision classée     | Terminer 7 matchs en Compétitif sur la semaine avec au moins 13 headshots par match             | AIM         | Compétitif | 7 × 13    | 7 × 17    |
| `VERY_HARD_COMP_ASSISTS`     | Ange gardien          | Cumuler 75 assists en Compétitif sur la semaine                                                 | SUPPORT     | Compétitif | 75        | 98        |
| `VERY_HARD_COMP_DAMAGE`      | Déluge                | Cumuler 31 000 dégâts en Compétitif sur la semaine                                              | DAMAGE      | Compétitif | 31 000    | 49 000    |
| `VERY_HARD_COMP_ADR_GAMES`   | Pression classée      | Terminer 7 matchs en Compétitif sur la semaine avec au moins 145 d'ADR par match                | DAMAGE      | Compétitif | 7 × 145   | 7 × 170   |
| `VERY_HARD_COMP_WINS`        | Victoires classées    | Remporter 7 matchs en Compétitif sur la semaine                                                 | VICTORY     | Compétitif | 7         | 10        |
| `VERY_HARD_CONSISTENCY`      | Régularité classée    | Remporter au moins un match en Compétitif sur 4 jours différents de la semaine                  | VICTORY     | Compétitif | 4         | 5         |
| `VERY_HARD_DAY_COMP_WINS`    | Journée parfaite      | Remporter 3 matchs en Compétitif dans une même journée de la semaine                            | VICTORY     | Compétitif | 3         | 4         |
| `VERY_HARD_COMP_DAILY`       | Classé chaque jour    | Jouer au moins un match en Compétitif sur 6 jours différents de la semaine                      | CONSISTENCY | Compétitif | 6         | 7         |
| `VERY_HARD_COMP_SCORE_GAMES` | Score tenu            | Terminer 5 matchs en Compétitif sur la semaine avec au moins 5 000 de score de combat par match | CONSISTENCY | Compétitif | 5 × 5 000 | 6 × 5 000 |
| `VERY_HARD_DM_KILLS`         | Roi de l'entraînement | Cumuler 500 kills en Deathmatch sur la semaine                                                  | TRAINING    | Deathmatch | 500       | 600       |
| `VERY_HARD_DM_KILL_GAMES`    | Roi du Deathmatch     | Terminer 12 matchs en Deathmatch sur la semaine avec au moins 28 kills par match                | TRAINING    | Deathmatch | 12 × 28   | 15 × 33   |

Répartition : MATCHES 2, PERFORMANCE 6, AIM 2, SUPPORT 1, DAMAGE 2, TRAINING 2, VICTORY 3, CONSISTENCY 2.

---

## Les échelles d'un palier à l'autre

Onze questions se posent à plusieurs paliers, et leur cible monte d'environ un quart à chaque
échelon. Une escouade qui progresse retrouve donc le défi qu'elle suivait, plus haut.

| Question                            | EASY   | NORMAL | MEDIUM | HARD   | VERY_HARD   |
| ----------------------------------- | ------ | ------ | ------ | ------ | ----------- |
| Parties en Compétitif ou Non classé | 3      | 4      | 5      | 9      | —           |
| Parties tous modes                  | 15     | 18     | 22     | 28     | —           |
| Rounds                              | 60     | 75     | 95     | 145    | 180         |
| Kills                               | 100    | 125    | 155    | 195    | 245         |
| Score                               | 25 000 | 30 000 | 38 000 | 48 000 | 60 000      |
| Parties à N kills                   | 3 × 13 | 4 × 14 | 5 × 15 | 6 × 16 | 7 × 17      |
| Headshots                           | 40     | 50     | 65     | 80     | 100         |
| Parties à N headshots               | 3 × 8  | 4 × 10 | 5 × 11 | 6 × 12 | 7 × 13      |
| Assists                             | 30     | 38     | 48     | 60     | 75          |
| Dégâts                              | 13 000 | 16 000 | 20 000 | 25 000 | 31 000      |
| Jours joués                         | 3      | 4      | 5      | 6      | 6 en classé |

Les cibles VERY_HARD portent sur le Compétitif seul, ce qui les rend plus dures que le nombre ne le
laisse croire.

## Ce qui reste connu et assumé

**Le Team Deathmatch et le Skirmish sont peu joués.** Les défis qui les exigent seront ratés plus
souvent que les autres, quel que soit leur nombre. Ils sont conservés comme incitation à varier les
modes, pas comme objectifs réguliers.

**Trois défis ne séparent pas leurs deux niveaux.** `NORMAL_MODE_VARIETY`, `HARD_MODE_VARIETY`,
`MEDIUM_AGENT_WINS`, `VERY_HARD_DAY_COMP_MATCHES` et `DAILY_TWO_MODES` portent la même valeur des
deux côtés : la question qu'ils posent — jouer dans N modes, gagner avec N agents — n'a pas assez de
marge pour distinguer deux niveaux sans devenir absurde.

**Les cumuls « tous modes confondus » sur les headshots, les assists et les dégâts sont des défis
Compétitif déguisés**, puisque le Deathmatch et le Skirmish n'en produisent aucun. Ils restent
écrits ainsi pour laisser le Team Deathmatch y contribuer.
