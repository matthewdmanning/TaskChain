# Results artifact

Each test run that matters produces one results artifact. It is part of the strategy.

- Start from [results-template.html](results-template.html). Copy it. Never edit the template in place.
- Fill in only the JSON block. Keep every key. Use `not-run` for work you did not do.
- Save the copy as `.scratch/test-results/<yyyy-mm-dd>-<branch>.html`.
- Publish the copy as an artifact when the run is evidence for a pull request or a release.
- Show a visual regression for each screen with a failed screenshot test.
- Also show a visual regression for each screen that a changed file is expected to affect, even when the test did not fail.
- Use a frame strip for a dynamic regression, such as the timer, an animation or a drag. Capture the frames on the device at fixed times.
- In the published copy, embed every image as a `data:` URI. The page cannot load other images.
- Never invent an image. An empty field shows "No image", which is itself a gap to report.
- Report device evidence apart from JVM evidence.
