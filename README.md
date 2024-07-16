# Navigation

Relatively simple tool for navigation in a lot of kotlin platforms:

![JVM](https://img.shields.io/badge/JVM-red?style=for-the-badge&logo=openjdk&logoColor=white)
![Android](https://img.shields.io/badge/Android-green?style=for-the-badge&logo=android&logoColor=white)
![Js](https://img.shields.io/badge/JavaScript-323330?style=for-the-badge&logo=javascript&logoColor=F7DF1E)
![ARM x64](https://img.shields.io/badge/ARMx64-0091BD?style=for-the-badge&logo=arm&logoColor=F7DF1E)
![Linux x64](https://img.shields.io/badge/Linuxx64-FCC624?style=for-the-badge&logo=linux&logoColor=F7DF1E)

[![KDocs](https://img.shields.io/badge/KDocs-323330?style=for-the-badge&logo=Kotlin&logoColor=7F52FF)](https://insanusmokrassar.github.io/navigation/)
[![Tutorials](https://img.shields.io/badge/Tutorials-0288D1?style=for-the-badge&logo=mkdocs&logoColor=white)](https://docs.inmo.dev/navigation/index.html)

It is strongly recommended to open tutorial after completing reading of this readme to understand how to use library

## Artifacts

There are several types of artifacts:

* Core - only base functionality: nodes, chains, navigation repositories, etc.
* Compose - Core + `Compose` wrappers for navigation node
* MVVM - Core + MVVM wrappers:
    * [ViewModel](./mvvm/src/commonMain/kotlin/ViewModel.kt)
    * [ComposeView](./mvvm/src/commonMain/kotlin/compose/ComposeView.kt)
    * Platforms-specific realizations of `View`s

## Installation

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/dev.inmo/navigation.core/badge.svg)](https://maven-badges.herokuapp.com/maven-central/dev.inmo/navigation.core)

### Gradle

```groovy
implementation "dev.inmo:navigation.mvvm:$navigation_version"
```

### Maven

```xml
<dependency>
  <groupId>dev.inmo</groupId>
  <artifactId>navigation.mvvm</artifactId>
  <version>${navigation_version}</version>
</dependency>
```
