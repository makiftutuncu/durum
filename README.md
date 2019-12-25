# Dürüm 🌯

Dürüm (Turkish for wraps, as in food) is an HTTP wrapper for Scala.

It helps generalize the behavior of handling an HTTP request in a server application. Dürüm provides abstractions for common operations like logging, timing, authorization etc. It has no external dependency and expects you to provide concrete implementations for the operations it supports.

More documentation and examples are coming very soon.

## Table of Contents

1. [Installation](#installation)
2. [Contributing](#contributing)
3. [License](#license)

## Installation

| Latest Version |
| -------------- |
| 0.1.0          |

Dürüm is *not yet* published to Maven Central. Once it is, replace `version` and add following to your `build.sbt` in order to add Dürüm to your project:

```scala
libraryDependencies += "dev.akif" %% "durum" % "{version}"
```

## Contributing

All contributions are welcome. Please feel free to send a pull request. Thank you.

## License

Dürüm is licensed with MIT License. See [LICENSE.md](https://github.com/makiftutuncu/durum/blob/master/LICENSE.md) for details.
