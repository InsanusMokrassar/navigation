# Navigation Changelog

## 0.0.17

* Fixes in `restoreHierarchy`
* Fixes in `createSubChain`

## 0.0.16

* `Versions`:
  * `MicroUtils`: `0.17.5`
* `NavigationChain` got its own optional id
* `NavigationChain` and `NavigationNode` got `findChain` extension
* `NavigationChain` and `NavigationNode` got `findNode` extension
* On restoring of hierarchy it is possible to remove the chains which do not fit restoring one
* Add opportunity to walk across whole navigation tree

## 0.0.15

* `Versions`:
  * `MicroUtils`: `0.17.2`

## 0.0.14

* `Versions`:
  * `Kotlin`: `1.8.10`
  * `Serialization`: `1.5.0`
  * `MicroUtils`: `0.17.0`
  * `KSLog`: `1.0.0`
  * `UUID`: `0.7.0`

## 0.0.13

* `Common`:
  * `NavigationChain` now have more useful logs for different methods
  * Reworked `push`, `drop` and `start` methods of `NavigationChain`
  * `NavigationNode` now have more useful logs object
  * Fixes in `NavigationNode.start`
  * Fixes in `NavigationChain#onNodesStackDiffFlow` and all subsequent diff flows
  * Fixes in `NavigationNode#onChainsStackDiffFlow` and all subsequent diff flows
* `Android`:
  * `AndroidFragmentNode` now will automatically attach to any found view with its navigation tag equal to `Config#id`

## 0.0.12

* Rewrite chain lists with immutable stacks

## 0.0.11

* `JS`:
  * `JsNavigationNode` got `htmlElementStateFlow` which is automatically updated when node is in resume state

## 0.0.10

* `Versions`:
  * `MicroUtils`: `0.16.5`
* Fixes in using of `rootChain` in `restoreHierarchy`
* `JS`:
  * Add more logs in `initNavigation`

## 0.0.9

* `JS`:
  * `initNavigation` got variant with opportunity to pass root chain

## 0.0.8

* Extended support of JS platform:
  * Extensions `NavigationNodeDefaultConfig#htmlElement` (`orNull`/`orThrow`)
  * `CookiesNavigationConfigsRepo`
  * `initNavigation` function for simple navigation enabling
  * `JsNavigationNode` abstraction as an analogue of android navigation node

## 0.0.7

* `NevigationChain` now uses regular mutable list instead of array deque
* `Versions`:
  * `Kotlin`: `1.7.21`
  * `MicroUtils`: `0.14.4`
  * `UUID`: `0.6.0`

## 0.0.6

* `Versions`:
  * `Kotlin`: `1.7.20`
  * `Serialization`: `1.4.1`
  * `MicroUtils`: `0.13.2`
  * `KSLog`: `0.5.3`
* Internal optimizations

## 0.0.5

**THIS RELEASE CONTAINS BREAKING CHANGES**

* `NavigationNode` now use two types: `Config : Base` and `Base`

## 0.0.4

* All `NavigationNode`s now have `configState` with type `StateFlow`. Under the hood any node may implement mutability
  of this flow as it is required

## 0.0.3

* `Versions`:
  * `MicroUtils`: `0.13.1`
* Fixes and improvements in android part

## 0.0.2

* `Versions`:
  * `MicroUtils`: `0.13.0`
  * `KSLog`: `0.5.2`
* Several tools for android navigation have been added
* Small sample for android have been added

## 0.0.1

Project inited
