# README tour

Films the animated tour (`docs/media/tour.webp`) and takes the phone screens (`docs/media/screen-*.png`) of the root README, from the real app on a copy of a squad's database.

```bash
tools/tour/tour.sh path/to/valoquests-dump.sql.gz
```

About three minutes, most of it building the images. Then look at the new media and commit them.

The dump is a plain `pg_dump` of a ValoQuests database, gzipped, such as the server's nightly backup. It is restored into a throwaway database and never committed. A campaign in its second half makes the best film: a built-up base, settled weeks, a mission report to open. Player names show in the media, which are public: use a squad that agrees to it.

## What it needs

- Docker, as for `make up`.
- Playwright's Chromium (`cd frontend && npx playwright install chromium`) and Node 22.
- Python 3 with Pillow, built with WebP support.
- Port 8100 free. A running `make up` or `make db` can stay up: the tour stack has its own project name, database and port.

## What it does

1. Builds the images of `compose.yaml` and starts the database as the `valoquests-tour` project (`compose.tour.yaml`: app on 8100, database not published), restores the dump into it, then starts the backend and the app. No scheduled job runs and the Henrik key is a placeholder, so the squad stays exactly as the dump left it.
2. `record.mjs` films `stage.html`: title cards, captions, and the app in a browser and a phone, clicked through like a visitor would, as a returning visitor who has already closed the latest mission report. `encode_tour.py` turns the frames into a 15 fps WebP, about 3 MB.
3. `shots.mjs` takes the phone screens and `frame_screens.py` draws the phone around them.
4. Stops the stack and drops its database, even on a failure (the logs of a failed run are kept, the path is printed).

The Henrik API and the development database are never touched. The app forbids being framed; only the tour's browser lifts that header, for the stage's two iframes.

## Changing it

| To change                        | Edit                                                                   |
| -------------------------------- | ---------------------------------------------------------------------- |
| The scenes, captions, clicks     | `record.mjs`, and `stage.html` for the cards and the look              |
| Which screens the README shows   | `shots.mjs`, then the `<img>` strip of the root README                 |
| What the visitor has already seen | `tour.utils.mjs`                                                      |
| Frame rate, quality, phone shape | the constants at the top of `encode_tour.py` and `frame_screens.py`    |
| Port, sizes                      | `tour.constants.mjs`, and `compose.tour.yaml` for the port             |

The scenes click through the app's own classes (`.overview__report-button`, `.page-header__menu-button`, `a.sidebar__nav-item`...) and scroll its `.page-body`: a renamed class breaks the take, and the error names it.
