# Catalogue de défis — proposition

121 défis : **100 hebdomadaires** (20 par difficulté) et **21 quotidiens**.
Règles d'écriture dans `CHALLENGES.md`, économie dans `GAMEPLAY.md`.

## Ce qui a changé depuis la première version

**« Partie longue » disparaît du vocabulaire.** Chaque défi nomme ses modes : « en Compétitif ou Non
classé ». Premier sort du filtre pour que le libellé reste court et vrai ; à remettre le jour où
l'escouade y joue.

**Aucun défi ne peut plus être détruit par une seule mauvaise partie.** Tes deux remarques, sur les
enchaînements et sur les K/D et ADR tenus sur X parties, désignent le même défaut : un objectif que
tu perds sans pouvoir le rattraper. Les 6 enchaînements et les 11 taux hebdomadaires sont supprimés
et remplacés par des « terminer N parties avec au moins X », où rater une partie ne coûte que cette
partie.

**Les volumes sont divisés par trois à quatre.** Les anciennes cibles venaient du catalogue existant,
qui était calibré beaucoup trop haut. Plafond de **5 parties par semaine en Compétitif ou Non classé**.
Le plafond ne s'applique pas au Deathmatch ni au Team Deathmatch : une partie y dure 9 minutes contre
35, donc 10 Deathmatch coûtent moins de temps que 3 compétitives.

**Les victoires suivent.** Remporter 12 parties devient remporter 2 à 5 selon la difficulté.

**Le mode est toujours nommé.** « 30 kills dans une même journée » devient « 30 kills dans une même
journée, tous modes confondus ».

## Budget hebdomadaire de référence

Toutes les cibles ci-dessous sont calées sur ce que joue réellement une escouade au palier **Normal**,
par joueur et par semaine :

| | Parties | Kills | Headshots | Assists | Rounds |
|---|---|---|---|---|---|
| Compétitif ou Non classé | 8 | 120 | 72 | 48 | 160 |
| Deathmatch | 15 | 405 | 180 | — | — |
| Team Deathmatch | 6 | 180 | 78 | — | — |
| Tous modes | 29 | — | — | — | — |

Une cible EASY vaut environ 35 % de ce budget, NORMAL 50 %, MEDIUM 65 %, HARD 80 %, VERY_HARD 100 %
sur un budget compétitif seul plus étroit.

## Comment lire les tableaux

**Base** : la cible telle qu'elle est écrite dans le JSON, pour une escouade au palier **Normal**
(référence 5 300). Recalculée au lancement de chaque campagne, jamais affichée telle quelle.

| Marque | Échelle |
|---|---|
| `VOL` | facteur de volume — référence de la campagne / 5 300, borné à [0,4 ; 3,0] |
| `TAL` | ancre de talent — médiane de l'escouade sur 9 mois × coefficient de difficulté |
| `VOL+TAL` | le nombre de parties suit `VOL`, la barre par partie suit `TAL` |
| `FIXE` | jamais mis à l'échelle — jours de la semaine, agents, nombre de modes |

**État** : `≈` code existant, cible recalibrée. `C` code existant, mode élargi au Non classé.
`R` code existant, forme de calcul changée. `N` nouveau. `X` supprimé.

---

## EASY — 20 défis

| Code | Cat. | Défi | Calcul | Base | Échelle | Excl. | État |
|---|---|---|---|---|---|---|---|
| `EASY_LONG_MATCHES` | MATCHES | Jouer 3 parties en Compétitif ou Non classé | SUM | 3 | VOL | LONG_MATCHES | C |
| `EASY_ANY_MATCHES` | MATCHES | Jouer 10 parties, tous modes confondus | SUM | 10 | VOL | ANY_MATCHES | ≈ |
| `EASY_LONG_ROUNDS` | MATCHES | Jouer 55 rounds en Compétitif ou Non classé | SUM | 55 | VOL | LONG_ROUNDS | C |
| `EASY_LONG_KILLS` | PERFORMANCE | Réaliser 40 kills en Compétitif ou Non classé | SUM | 40 | VOL | LONG_KILLS | C |
| `EASY_LONG_SCORE` | PERFORMANCE | Cumuler 12 000 de score en Compétitif ou Non classé | SUM | 12 000 | VOL | LONG_SCORE | C |
| `EASY_LONG_KILL_GAMES` | PERFORMANCE | Terminer 3 parties en Compétitif ou Non classé avec 13 kills ou plus | COUNT_MATCHES | 3 × 13 | VOL+TAL | LONG_KILL_GAMES | N |
| `EASY_LONG_HEADSHOTS` | AIM | Réaliser 25 headshots en Compétitif ou Non classé | SUM | 25 | VOL | LONG_HEADSHOTS | C |
| `EASY_DM_HEADSHOTS` | AIM | Réaliser 60 headshots en Deathmatch | SUM | 60 | VOL | DM_HEADSHOTS | ≈ |
| `EASY_LONG_ASSISTS` | SUPPORT | Réaliser 15 assists en Compétitif ou Non classé | SUM | 15 | VOL | LONG_ASSISTS | C |
| `EASY_LONG_DAMAGE` | DAMAGE | Infliger 7 000 dégâts en Compétitif ou Non classé | SUM | 7 000 | VOL | LONG_DAMAGE | N |
| `EASY_DM_ROUTINE` | TRAINING | Jouer 5 Deathmatch | SUM | 5 | VOL | DM_MATCHES | ≈ |
| `EASY_TDM_ROUTINE` | TRAINING | Jouer 3 Team Deathmatch | SUM | 3 | VOL | TDM_MATCHES | ≈ |
| `EASY_DM_KILLS` | TRAINING | Réaliser 140 kills en Deathmatch | SUM | 140 | VOL | DM_KILLS | N |
| `EASY_MIXED_ROUTINE` | TRAINING | Jouer 2 parties en Compétitif ou Non classé et 3 Deathmatch | ALL | 2 + 3 | VOL | MIXED_ROUTINE | N |
| `EASY_LONG_WINS` | VICTORY | Remporter 2 parties en Compétitif ou Non classé | SUM | 2 | VOL | LONG_WINS | N |
| `EASY_TDM_WINS` | VICTORY | Remporter 2 Team Deathmatch | SUM | 2 | VOL | TDM_WINS | ≈ |
| `EASY_PLAY_DAYS` | CONSISTENCY | Jouer 4 jours différents | DISTINCT_COUNT | 4 | FIXE | PLAY_DAYS | ≈ |
| `EASY_LONG_KD_GAMES` | CONSISTENCY | Terminer 3 parties en Compétitif ou Non classé avec un K/D de 0,90 ou plus | COUNT_MATCHES | 3 × 0,90 | VOL+TAL | LONG_KD_GAMES | C |
| `EASY_AGENT_VARIETY` | AGENT | Jouer 3 agents différents en Compétitif ou Non classé | DISTINCT_COUNT | 3 | FIXE | AGENT_VARIETY | C |
| `EASY_DAY_BEST_KILLS` | VARIETY | Réaliser 30 kills dans une même journée, tous modes confondus | MAX_GROUP | 30 | VOL | DAY_BEST_KILLS | N |

---

## NORMAL — 20 défis

| Code | Cat. | Défi | Calcul | Base | Échelle | Excl. | État |
|---|---|---|---|---|---|---|---|
| `NORMAL_LONG_MATCHES` | MATCHES | Jouer 4 parties en Compétitif ou Non classé | SUM | 4 | VOL | LONG_MATCHES | N |
| `NORMAL_ANY_MATCHES` | MATCHES | Jouer 15 parties, tous modes confondus | SUM | 15 | VOL | ANY_MATCHES | N |
| `NORMAL_LONG_ROUNDS` | MATCHES | Jouer 80 rounds en Compétitif ou Non classé | SUM | 80 | VOL | LONG_ROUNDS | C |
| `NORMAL_LONG_KILLS` | PERFORMANCE | Réaliser 60 kills en Compétitif ou Non classé | SUM | 60 | VOL | LONG_KILLS | C |
| `NORMAL_LONG_SCORE` | PERFORMANCE | Cumuler 17 000 de score en Compétitif ou Non classé | SUM | 17 000 | VOL | LONG_SCORE | C |
| `NORMAL_LONG_KILL_GAMES` | PERFORMANCE | Terminer 4 parties en Compétitif ou Non classé avec 15 kills ou plus | COUNT_MATCHES | 4 × 15 | VOL+TAL | LONG_KILL_GAMES | N |
| `NORMAL_LONG_HEADSHOTS` | AIM | Réaliser 36 headshots en Compétitif ou Non classé | SUM | 36 | VOL | LONG_HEADSHOTS | C |
| `NORMAL_LONG_HS_GAMES` | AIM | Terminer 4 parties en Compétitif ou Non classé avec 9 headshots ou plus | COUNT_MATCHES | 4 × 9 | VOL+TAL | LONG_HS_GAMES | C |
| `NORMAL_LONG_ASSISTS` | SUPPORT | Réaliser 24 assists en Compétitif ou Non classé | SUM | 24 | VOL | LONG_ASSISTS | C |
| `NORMAL_LONG_DAMAGE` | DAMAGE | Infliger 10 000 dégâts en Compétitif ou Non classé | SUM | 10 000 | VOL | LONG_DAMAGE | C |
| `NORMAL_LONG_ADR_GAMES` | DAMAGE | Terminer 4 parties en Compétitif ou Non classé à 130 d'ADR ou plus | COUNT_MATCHES | 4 × 130 | VOL+TAL | LONG_ADR_GAMES | N |
| `NORMAL_DM_KILLS` | TRAINING | Réaliser 200 kills en Deathmatch | SUM | 200 | VOL | DM_KILLS | ≈ |
| `NORMAL_TDM_KILLS` | TRAINING | Réaliser 90 kills en Team Deathmatch | SUM | 90 | VOL | TDM_KILLS | ≈ |
| `NORMAL_DM_KILL_GAMES` | TRAINING | Terminer 6 Deathmatch avec 27 kills ou plus | COUNT_MATCHES | 6 × 27 | VOL+TAL | DM_KILL_GAMES | N |
| `NORMAL_LONG_WINS` | VICTORY | Remporter 3 parties en Compétitif ou Non classé | SUM | 3 | VOL | LONG_WINS | C |
| `NORMAL_DAY_WINS` | VICTORY | Remporter 2 parties dans une même journée, tous modes confondus | MAX_GROUP | 2 | FIXE | DAY_BEST_WINS | N |
| `NORMAL_PLAY_DAYS` | CONSISTENCY | Jouer 5 jours différents | DISTINCT_COUNT | 5 | FIXE | PLAY_DAYS | ≈ |
| `NORMAL_LONG_KD_GAMES` | CONSISTENCY | Terminer 4 parties en Compétitif ou Non classé avec un K/D de 1,00 ou plus | COUNT_MATCHES | 4 × 1,00 | VOL+TAL | LONG_KD_GAMES | N |
| `NORMAL_MODE_VARIETY` | VARIETY | Jouer au moins une partie dans 4 modes différents | DISTINCT_COUNT | 4 | FIXE | MODE_VARIETY | ≈ |
| `NORMAL_DAY_BEST_KILLS` | VARIETY | Réaliser 45 kills dans une même journée, tous modes confondus | MAX_GROUP | 45 | VOL | DAY_BEST_KILLS | N |

---

## MEDIUM — 20 défis

| Code | Cat. | Défi | Calcul | Base | Échelle | Excl. | État |
|---|---|---|---|---|---|---|---|
| `MEDIUM_LONG_MATCHES` | MATCHES | Jouer 5 parties en Compétitif ou Non classé | SUM | 5 | VOL | LONG_MATCHES | C |
| `MEDIUM_ANY_MATCHES` | MATCHES | Jouer 20 parties, tous modes confondus | SUM | 20 | VOL | ANY_MATCHES | ≈ |
| `MEDIUM_LONG_ROUNDS` | MATCHES | Jouer 105 rounds en Compétitif ou Non classé | SUM | 105 | VOL | LONG_ROUNDS | C |
| `MEDIUM_LONG_KILLS` | PERFORMANCE | Réaliser 80 kills en Compétitif ou Non classé | SUM | 80 | VOL | LONG_KILLS | N |
| `MEDIUM_LONG_SCORE` | PERFORMANCE | Cumuler 22 000 de score en Compétitif ou Non classé | SUM | 22 000 | VOL | LONG_SCORE | C |
| `MEDIUM_LONG_KILL_GAMES` | PERFORMANCE | Terminer 5 parties en Compétitif ou Non classé avec 16 kills ou plus | COUNT_MATCHES | 5 × 16 | VOL+TAL | LONG_KILL_GAMES | C |
| `MEDIUM_LONG_ACS_GAMES` | PERFORMANCE | Terminer 5 parties en Compétitif ou Non classé à 225 d'ACS ou plus | COUNT_MATCHES | 5 × 225 | VOL+TAL | LONG_ACS_GAMES | N |
| `MEDIUM_LONG_HS_GAMES` | AIM | Terminer 5 parties en Compétitif ou Non classé avec 10 headshots ou plus | COUNT_MATCHES | 5 × 10 | VOL+TAL | LONG_HS_GAMES | C |
| `MEDIUM_DM_HS_GAMES` | AIM | Terminer 8 Deathmatch avec 13 headshots ou plus | COUNT_MATCHES | 8 × 13 | VOL+TAL | DM_HS_GAMES | N |
| `MEDIUM_LONG_ASSISTS` | SUPPORT | Réaliser 30 assists en Compétitif ou Non classé | SUM | 30 | VOL | LONG_ASSISTS | N |
| `MEDIUM_LONG_DAMAGE` | DAMAGE | Infliger 13 500 dégâts en Compétitif ou Non classé | SUM | 13 500 | VOL | LONG_DAMAGE | N |
| `MEDIUM_LONG_ADR_GAMES` | DAMAGE | Terminer 5 parties en Compétitif ou Non classé à 140 d'ADR ou plus | COUNT_MATCHES | 5 × 140 | VOL+TAL | LONG_ADR_GAMES | R |
| `MEDIUM_DM_MATCHES` | TRAINING | Jouer 10 Deathmatch | SUM | 10 | VOL | DM_MATCHES | ≈ |
| `MEDIUM_TDM_MATCHES` | TRAINING | Jouer 5 Team Deathmatch | SUM | 5 | VOL | TDM_MATCHES | ≈ |
| `MEDIUM_AIM_TRAINER` | TRAINING | Jouer 6 Deathmatch et 4 Team Deathmatch | ALL | 6 + 4 | VOL | AIM_TRAINING | ≈ |
| `MEDIUM_TDM_WINS` | VICTORY | Remporter 4 Team Deathmatch | SUM | 4 | VOL | TDM_WINS | ≈ |
| `MEDIUM_WIN_DAYS` | VICTORY | Remporter une partie en Compétitif ou Non classé 3 jours différents | DISTINCT_COUNT | 3 | FIXE | WIN_DAYS | N |
| `MEDIUM_PLAY_DAYS` | CONSISTENCY | Jouer 6 jours différents | DISTINCT_COUNT | 6 | FIXE | PLAY_DAYS | ≈ |
| `MEDIUM_LONG_KD_GAMES` | CONSISTENCY | Terminer 5 parties en Compétitif ou Non classé avec un K/D de 1,10 ou plus | COUNT_MATCHES | 5 × 1,10 | VOL+TAL | LONG_KD_GAMES | N |
| `MEDIUM_AGENT_WINS` | AGENT | Remporter une partie en Compétitif ou Non classé avec 3 agents différents | DISTINCT_COUNT | 3 | FIXE | AGENT_WIN_VARIETY | C |

---

## HARD — 20 défis

| Code | Cat. | Défi | Calcul | Base | Échelle | Excl. | État |
|---|---|---|---|---|---|---|---|
| `HARD_LONG_ROUNDS` | MATCHES | Jouer 130 rounds en Compétitif ou Non classé | SUM | 130 | VOL | LONG_ROUNDS | N |
| `HARD_ANY_MATCHES` | MATCHES | Jouer 24 parties, tous modes confondus | SUM | 24 | VOL | ANY_MATCHES | N |
| `HARD_LONG_KILLS` | PERFORMANCE | Réaliser 100 kills en Compétitif ou Non classé | SUM | 100 | VOL | LONG_KILLS | C |
| `HARD_LONG_SCORE` | PERFORMANCE | Cumuler 27 000 de score en Compétitif ou Non classé | SUM | 27 000 | VOL | LONG_SCORE | C |
| `HARD_LONG_KILL_GAMES` | PERFORMANCE | Terminer 6 parties en Compétitif ou Non classé avec 18 kills ou plus | COUNT_MATCHES | 6 × 18 | VOL+TAL | LONG_KILL_GAMES | N |
| `HARD_LONG_ACS_GAMES` | PERFORMANCE | Terminer 6 parties en Compétitif ou Non classé à 250 d'ACS ou plus | COUNT_MATCHES | 6 × 250 | VOL+TAL | LONG_ACS_GAMES | R |
| `HARD_DAY_BEST_KILLS` | PERFORMANCE | Réaliser 80 kills dans une même journée, tous modes confondus | MAX_GROUP | 80 | VOL | DAY_BEST_KILLS | N |
| `HARD_LONG_HEADSHOTS` | AIM | Réaliser 58 headshots en Compétitif ou Non classé | SUM | 58 | VOL | LONG_HEADSHOTS | C |
| `HARD_LONG_HS_GAMES` | AIM | Terminer 6 parties en Compétitif ou Non classé avec 11 headshots ou plus | COUNT_MATCHES | 6 × 11 | VOL+TAL | LONG_HS_GAMES | N |
| `HARD_LONG_ASSISTS` | SUPPORT | Réaliser 38 assists en Compétitif ou Non classé | SUM | 38 | VOL | LONG_ASSISTS | C |
| `HARD_LONG_DAMAGE` | DAMAGE | Infliger 17 000 dégâts en Compétitif ou Non classé | SUM | 17 000 | VOL | LONG_DAMAGE | C |
| `HARD_LONG_ADR_GAMES` | DAMAGE | Terminer 6 parties en Compétitif ou Non classé à 155 d'ADR ou plus | COUNT_MATCHES | 6 × 155 | VOL+TAL | LONG_ADR_GAMES | N |
| `HARD_DM_KILL_GAMES` | TRAINING | Terminer 10 Deathmatch avec 32 kills ou plus | COUNT_MATCHES | 10 × 32 | VOL+TAL | DM_KILL_GAMES | ≈ |
| `HARD_TDM_KILL_GAMES` | TRAINING | Terminer 5 Team Deathmatch avec 35 kills ou plus | COUNT_MATCHES | 5 × 35 | VOL+TAL | TDM_KILL_GAMES | ≈ |
| `HARD_AIM_MARATHON` | TRAINING | Jouer 10 Deathmatch et 6 Team Deathmatch | ALL | 10 + 6 | VOL | AIM_TRAINING | N |
| `HARD_LONG_WINS` | VICTORY | Remporter 5 parties en Compétitif ou Non classé | SUM | 5 | VOL | LONG_WINS | C |
| `HARD_WIN_DAYS` | VICTORY | Remporter une partie en Compétitif ou Non classé 4 jours différents | DISTINCT_COUNT | 4 | FIXE | WIN_DAYS | N |
| `HARD_DAILY_PLAYER` | CONSISTENCY | Jouer les 7 jours de la semaine | DISTINCT_COUNT | 7 | FIXE | PLAY_DAYS | ≈ |
| `HARD_LONG_KD_GAMES` | CONSISTENCY | Terminer 6 parties en Compétitif ou Non classé avec un K/D de 1,20 ou plus | COUNT_MATCHES | 6 × 1,20 | VOL+TAL | LONG_KD_GAMES | R |
| `HARD_MODE_VARIETY` | VARIETY | Jouer au moins une partie dans 5 modes différents | DISTINCT_COUNT | 5 | FIXE | MODE_VARIETY | N |

---

## VERY_HARD — 20 défis

Seule difficulté où le **Compétitif** est exigé. Deux entrées y échappent volontairement
(`VERY_HARD_TRAINING_MASTER`, `VERY_HARD_DM_KILL_GAMES`) : un joueur qui ne touche jamais au classé
garde une chance sur dix de tomber sur un VERY_HARD qu'il peut valider.

| Code | Cat. | Défi | Calcul | Base | Échelle | Excl. | État |
|---|---|---|---|---|---|---|---|
| `VERY_HARD_COMP_ROUNDS` | MATCHES | Jouer 120 rounds en Compétitif | SUM | 120 | VOL | COMP_ROUNDS | N |
| `VERY_HARD_DAY_COMP_MATCHES` | MATCHES | Jouer 3 parties compétitives dans une même journée | MAX_GROUP | 3 | FIXE | DAY_BEST_MATCHES | N |
| `VERY_HARD_COMP_KILLS` | PERFORMANCE | Réaliser 90 kills en Compétitif | SUM | 90 | VOL | COMP_KILLS | ≈ |
| `VERY_HARD_COMP_SCORE` | PERFORMANCE | Cumuler 25 000 de score en Compétitif | SUM | 25 000 | VOL | COMP_SCORE | ≈ |
| `VERY_HARD_COMP_KILL_GAMES` | PERFORMANCE | Terminer 5 compétitives avec 20 kills ou plus | COUNT_MATCHES | 5 × 20 | VOL+TAL | COMP_KILL_GAMES | ≈ |
| `VERY_HARD_COMP_ACS_GAMES` | PERFORMANCE | Terminer 5 compétitives à 280 d'ACS ou plus | COUNT_MATCHES | 5 × 280 | VOL+TAL | COMP_ACS_GAMES | ≈ |
| `VERY_HARD_COMP_KD_GAMES` | PERFORMANCE | Terminer 5 compétitives avec un K/D de 1,35 ou plus | COUNT_MATCHES | 5 × 1,35 | VOL+TAL | COMP_KD_GAMES | R |
| `VERY_HARD_DAY_BEST_KILLS` | PERFORMANCE | Réaliser 60 kills en Compétitif dans une même journée | MAX_GROUP | 60 | VOL | DAY_BEST_KILLS | N |
| `VERY_HARD_COMP_HEADSHOTS` | AIM | Réaliser 50 headshots en Compétitif | SUM | 50 | VOL | COMP_HEADSHOTS | ≈ |
| `VERY_HARD_COMP_HS_GAMES` | AIM | Terminer 5 compétitives avec 12 headshots ou plus | COUNT_MATCHES | 5 × 12 | VOL+TAL | COMP_HS_GAMES | N |
| `VERY_HARD_COMP_ASSISTS` | SUPPORT | Réaliser 35 assists en Compétitif | SUM | 35 | VOL | COMP_ASSISTS | ≈ |
| `VERY_HARD_COMP_DAMAGE` | DAMAGE | Infliger 15 000 dégâts en Compétitif | SUM | 15 000 | VOL | COMP_DAMAGE | ≈ |
| `VERY_HARD_COMP_ADR_GAMES` | DAMAGE | Terminer 5 compétitives à 170 d'ADR ou plus | COUNT_MATCHES | 5 × 170 | VOL+TAL | COMP_ADR_GAMES | R |
| `VERY_HARD_COMP_WINS` | VICTORY | Remporter 4 parties compétitives | SUM | 4 | VOL | COMP_WINS | N |
| `VERY_HARD_CONSISTENCY` | VICTORY | Remporter une compétitive 4 jours différents | DISTINCT_COUNT | 4 | FIXE | WIN_DAYS | ≈ |
| `VERY_HARD_DAY_COMP_WINS` | VICTORY | Remporter 3 compétitives dans une même journée | MAX_GROUP | 3 | FIXE | DAY_BEST_WINS | N |
| `VERY_HARD_COMP_DAILY` | CONSISTENCY | Jouer une compétitive 6 jours différents | DISTINCT_COUNT | 6 | FIXE | PLAY_DAYS | N |
| `VERY_HARD_COMP_SCORE_GAMES` | CONSISTENCY | Terminer 5 compétitives avec 5 000 de score de combat ou plus | COUNT_MATCHES | 5 × 5 000 | VOL+TAL | COMP_SCORE_GAMES | N |
| `VERY_HARD_TRAINING_MASTER` | TRAINING | Réaliser 300 kills en Deathmatch et 150 en Team Deathmatch | ALL | 300 + 150 | VOL | AIM_KILLS | ≈ |
| `VERY_HARD_DM_KILL_GAMES` | TRAINING | Terminer 12 Deathmatch avec 35 kills ou plus | COUNT_MATCHES | 12 × 35 | VOL+TAL | DM_KILL_GAMES | N |

---

## Quotidiens — 21 défis

Un seul est tiré par jour, commun à l'escouade, résolu le soir même. Tous réalisables en **une ou deux
parties**. Aucun n'exige du Compétitif. 21 entrées donnent trois semaines sans répétition.

| Code | Cat. | Défi | Calcul | Base | Échelle |
|---|---|---|---|---|---|
| `DAILY_ONE_LONG` | MATCHES | Jouer 1 partie en Compétitif ou Non classé | SUM | 1 | FIXE |
| `DAILY_TWO_MATCHES` | MATCHES | Jouer 2 parties, tous modes confondus | SUM | 2 | FIXE |
| `DAILY_LONG_WIN` | VICTORY | Remporter 1 partie en Compétitif ou Non classé | SUM | 1 | FIXE |
| `DAILY_TWO_WINS` | VICTORY | Remporter 2 parties, tous modes confondus | SUM | 2 | FIXE |
| `DAILY_TDM_WIN` | VICTORY | Remporter 1 Team Deathmatch | SUM | 1 | FIXE |
| `DAILY_LONG_KILLS` | PERFORMANCE | Terminer 1 partie en Compétitif ou Non classé avec 13 kills ou plus | COUNT_MATCHES | 1 × 13 | TAL |
| `DAILY_LONG_BIG_GAME` | PERFORMANCE | Terminer 1 partie en Compétitif ou Non classé avec 18 kills ou plus | COUNT_MATCHES | 1 × 18 | TAL |
| `DAILY_LONG_POSITIVE` | PERFORMANCE | Terminer 1 partie en Compétitif ou Non classé avec un K/D de 0,90 ou plus | COUNT_MATCHES | 1 × 0,90 | TAL |
| `DAILY_LONG_ACS` | PERFORMANCE | Terminer 1 partie en Compétitif ou Non classé à 190 d'ACS ou plus | COUNT_MATCHES | 1 × 190 | TAL |
| `DAILY_DAY_KILLS` | PERFORMANCE | Réaliser 30 kills dans la journée, tous modes confondus | SUM | 30 | VOL |
| `DAILY_LONG_HEADSHOTS` | AIM | Terminer 1 partie en Compétitif ou Non classé avec 8 headshots ou plus | COUNT_MATCHES | 1 × 8 | TAL |
| `DAILY_DM_HEADSHOTS` | AIM | Terminer 1 Deathmatch avec 12 headshots ou plus | COUNT_MATCHES | 1 × 12 | TAL |
| `DAILY_DAY_HEADSHOTS` | AIM | Réaliser 25 headshots dans la journée, tous modes confondus | SUM | 25 | VOL |
| `DAILY_DM_KILLS` | TRAINING | Terminer 1 Deathmatch avec 24 kills ou plus | COUNT_MATCHES | 1 × 24 | TAL |
| `DAILY_DM_DOUBLE` | TRAINING | Terminer 2 Deathmatch avec 20 kills ou plus chacun | COUNT_MATCHES | 2 × 20 | TAL |
| `DAILY_TDM_KILLS` | TRAINING | Terminer 1 Team Deathmatch avec 27 kills ou plus | COUNT_MATCHES | 1 × 27 | TAL |
| `DAILY_WARMUP` | TRAINING | Jouer 1 Deathmatch et 1 partie en Compétitif ou Non classé | ALL | 1 + 1 | FIXE |
| `DAILY_LONG_ASSISTS` | SUPPORT | Terminer 1 partie en Compétitif ou Non classé avec 6 assists ou plus | COUNT_MATCHES | 1 × 6 | TAL |
| `DAILY_LONG_ADR` | DAMAGE | Terminer 1 partie en Compétitif ou Non classé à 130 d'ADR ou plus | COUNT_MATCHES | 1 × 130 | TAL |
| `DAILY_DAY_DAMAGE` | DAMAGE | Infliger 4 500 dégâts dans la journée, tous modes confondus | SUM | 4 500 | VOL |
| `DAILY_TWO_MODES` | VARIETY | Jouer 2 modes différents dans la journée | DISTINCT_COUNT | 2 | FIXE |

---

## Les 9 défis supprimés

| Code | Raison |
|---|---|
| `NORMAL_WIN_STREAK` | enchaînement |
| `HARD_WIN_STREAK` | enchaînement |
| `VERY_HARD_WIN_STREAK` | enchaînement |
| `HARD_POSITIVE_KD_STREAK` | enchaînement |
| `NORMAL_MAIN_AGENT` | 12 parties avec le même agent, plafonné à 3 il ne veut plus rien dire |
| `VERY_HARD_MAIN_AGENT` | idem, 25 parties |
| `MEDIUM_AGENT_VARIETY` | 8 agents, plafonné à 3 il double `EASY_AGENT_VARIETY` |
| `HARD_AGENT_VARIETY` | idem, 10 agents |
| `VERY_HARD_AGENT_VARIETY` | idem, 12 agents |

Les taux hebdomadaires ne sont pas supprimés mais **changés de forme** : `MEDIUM_COMP_ADR`,
`HARD_WEEKLY_KD`, `HARD_COMP_ACS`, `VERY_HARD_WEEKLY_KD`, `VERY_HARD_COMP_ADR` deviennent des
« terminer N parties avec au moins X » sous leur nouveau nom. Même question posée, mais rattrapable.

Restent désactivés depuis `V28` : `VERY_HARD_ANY_MATCHES`, `VERY_HARD_DM_MATCHES`,
`VERY_HARD_TDM_MATCHES`, `HARD_DM_MATCHES`, `HARD_TDM_MATCHES`.

---

## Répartition

### Par catégorie

| Catégorie | EASY | NORMAL | MEDIUM | HARD | VERY_HARD | Quotidien | Total |
|---|---|---|---|---|---|---|---|
| MATCHES | 3 | 3 | 3 | 2 | 2 | 2 | 15 |
| PERFORMANCE | 3 | 3 | 4 | 5 | 6 | 5 | 26 |
| AIM | 2 | 2 | 2 | 2 | 2 | 3 | 13 |
| SUPPORT | 1 | 1 | 1 | 1 | 1 | 1 | 6 |
| DAMAGE | 1 | 2 | 2 | 2 | 2 | 2 | 11 |
| TRAINING | 4 | 3 | 3 | 3 | 2 | 4 | 19 |
| VICTORY | 2 | 2 | 2 | 2 | 3 | 3 | 14 |
| CONSISTENCY | 2 | 2 | 2 | 2 | 2 | 0 | 10 |
| AGENT | 1 | 0 | 1 | 0 | 0 | 0 | 2 |
| VARIETY | 1 | 2 | 0 | 1 | 0 | 1 | 5 |
| **Total** | **20** | **20** | **20** | **20** | **20** | **21** | **121** |

### Par mode exigé

| Mode | Hebdo | Quotidien |
|---|---|---|
| Compétitif ou Non classé | 49 | 9 |
| Deathmatch | 9 | 3 |
| Team Deathmatch | 6 | 2 |
| Tous modes | 14 | 6 |
| Composite | 4 | 1 |
| Compétitif seul | 18 | 0 |

Les 18 entrées Compétitif seul sont toutes en VERY_HARD. **0 en dessous.**

### Par mode de calcul

| Calcul | Nombre | Rattrapable ? |
|---|---|---|
| SUM | 58 | oui, cumulatif |
| COUNT_MATCHES | 38 | oui, une partie ratée ne coûte que cette partie |
| DISTINCT_COUNT | 13 | oui, sauf le dernier jour de la semaine |
| MAX_GROUP | 7 | oui, chaque journée est une nouvelle tentative |
| ALL | 5 | oui, cumulatif |
| RATIO | 0 | supprimé |
| MAX_STREAK | 0 | supprimé |

`ProgressMode.RATIO` et `ProgressMode.MAX_STREAK` n'ont plus aucune entrée. Comme `BASELINE` depuis
`V38`, les enums et leurs calculateurs restent dans le code sans être déclarés par le catalogue.

### Par travail de migration

| État | Nombre |
|---|---|
| `≈` code repris, cible recalibrée | 28 |
| `C` code repris, mode élargi au Non classé | 28 |
| `R` code repris, forme de calcul changée | 5 |
| `N` nouveau hebdomadaire | 39 |
| `N` nouveau quotidien | 21 |
| `X` supprimé | 9 |

Sur les 70 défis actifs actuels, **61 survivent** sous une forme ou une autre et 9 disparaissent.
5 autres restent désactivés depuis `V28`.

---

## Les ancres de talent utilisées

Calculées une fois sur neuf mois d'historique, médiane par joueur puis médiane de l'escouade, gelées
au lancement de la campagne. Les valeurs ci-dessous sont celles supposées pour ton escouade et servent
à écrire les bases des tableaux.

| Ancre | Valeur supposée | EASY | NORMAL | MEDIUM | HARD | VERY_HARD |
|---|---|---|---|---|---|---|
| Kills par partie Comp / Non classé | 15 | 13 | 15 | 16 | 18 | 20 |
| Headshots par partie Comp / Non classé | 9 | 8 | 9 | 10 | 11 | 12 |
| Assists par partie Comp / Non classé | 6 | 5 | 6 | 6 | 7 | 8 |
| Score par partie Comp / Non classé | 4 200 | 3 800 | 4 200 | 4 500 | 5 000 | 5 000 |
| K/D | 1,02 | 0,90 | 1,00 | 1,10 | 1,20 | 1,35 |
| ADR | 130 | 115 | 130 | 140 | 155 | 170 |
| ACS | 210 | 190 | 210 | 225 | 250 | 280 |
| Kills par Deathmatch | 27 | 24 | 27 | 29 | 32 | 35 |
| Headshots par Deathmatch | 12 | 11 | 12 | 13 | 14 | 15 |
| Kills par Team Deathmatch | 30 | 27 | 30 | 32 | 35 | 38 |

Coefficients appliqués : 0,90 / 1,00 / 1,08 / 1,18 / 1,32, et 0,85 pour le quotidien.

---

## Ce qui reste ouvert

**Les ancres de talent sont des suppositions.** Kills par partie, ADR, ACS : déduits des cibles
actuelles du catalogue, pas mesurés. Une fois `SquadCalibrationService` écrit, ils seront lus sur vos
vraies neuf mois et toutes les bases se recaleront d'elles-mêmes.

**Le budget hebdomadaire aussi.** J'ai retenu 8 parties longues, 15 Deathmatch et 6 Team Deathmatch
par joueur, d'après ce que tu m'as décrit. Si le vrai chiffre est plus bas, tout le tableau descend
proportionnellement, c'est un seul nombre à changer.

**Le Compétitif reste seul en VERY_HARD.** 18 entrées sur 20. Si tu veux en ouvrir davantage à un
joueur qui ne joue pas classé, dis-le et j'en bascule trois ou quatre en Deathmatch et Team
Deathmatch.
