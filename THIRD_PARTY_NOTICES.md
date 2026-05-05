# Third-Party Notices

Logic Gate Simulator includes third-party open-source software in its source code,
build artifacts, and packaged desktop distributions.

This notice is provided for attribution and license compliance. It is not a
substitute for the full license texts published by each upstream project.

## Runtime and Distributed Components

| Component | Version | License | Project / License |
|---|---:|---|---|
| Eclipse Temurin / OpenJDK Runtime | 21 | GPL-2.0 with Classpath Exception and related OpenJDK notices | https://adoptium.net/ |
| OpenJFX / JavaFX | 21 | GPL-2.0 with Classpath Exception | https://openjfx.io/ |
| Gson | 2.10.1 | Apache License 2.0 | https://github.com/google/gson |
| commonmark-java | 0.21.0 | BSD 2-Clause License | https://github.com/commonmark/commonmark-java |
| Ikonli | 12.3.1 | Apache License 2.0 | https://github.com/kordamp/ikonli |
| Material Design Icons | bundled via Ikonli | Pictogrammers Free License; icons and fonts under Apache License 2.0 | https://pictogrammers.com/docs/general/license/ |

## Notes

- The Windows, macOS, and Linux desktop packages are built with Eclipse Temurin
  JDK 21 in GitHub Actions.
- JavaFX is packaged with the application through Maven dependencies and
  jpackage.
- Ikonli is used to render icons in the JavaFX user interface.
- Material Design Icons are used through the Ikonli Material Design 2 icon pack.
- JUnit and Hamcrest are test-only dependencies and are not part of the runtime
  application distribution.

## License References

- Apache License 2.0: https://www.apache.org/licenses/LICENSE-2.0
- BSD 2-Clause License: https://opensource.org/license/bsd-2-clause
- GNU Classpath Exception: https://www.gnu.org/software/classpath/license.html
- OpenJFX: https://openjfx.io/
- Eclipse Temurin: https://adoptium.net/

