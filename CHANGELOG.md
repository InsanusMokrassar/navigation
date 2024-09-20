# Navigation Changelog

## 0.5.4

## 0.5.3

* `Versions`:
  * `Kotlin`: `1.9.23` -> `2.0.20`
  * `Serialization`: `1.6.3` -> `1.7.2`
  * `KSLog`: `1.3.4` -> `1.3.6`
  * `Compose`: `1.6.2` -> `1.7.0-alpha03`
  * `MicroUtils`: `0.21.0` -> `0.22.2`
  * `Android Compile SDK`: `34` -> `35`

## 0.5.2

* Add support for native targets

## 0.5.1

* `Versions`:
  * `MicroUtils`: `0.20.53` -> `0.21.0`

## 0.5.0

* `Versions`:
  * `Coroutines`: `1.8.0` -> `1.8.1`
  * `MicroUtils`: `0.20.43` -> `0.20.53`
  * `KSLog`: `1.3.3` -> `1.3.4`
  * `Koin`: `3.5.4` -> `3.5.6`
* `Core`:
  * Drop redundant `inline`s from extensions like `findInSubTree` to avoid efficiency degrading
* `Compose`:
  * Module has been created to provide support of `Compose`-oriented navigation
* `Sample`:
  * Sample has been fully rewritten to work with `Compose`-oriented way

## 0.4.1

* `Versions`:
  * `Kotlin`: `1.9.22` -> `1.9.23`
  * `MicroUtils`: `0.20.38` -> `0.20.43`
  * `KSLog`: `1.3.2` -> `1.3.3`
  * `UUID`: `0.8.2` -> `0.8.4`
  * `Koin`: `3.5.3` -> `3.5.4`

## 0.4.0

* `Versions`:
  * `Coroutines`: `1.7.3` -> `1.8.0`
  * `Serialization`: `1.6.2` -> `1.6.3`
  * `MicroUtils`: `0.20.31` -> `0.20.38`
* `Core`:
  * `Android`:
    * Add opportunity to configure `FragmentTransaction`

## 0.3.9

* `Versions`:
  * `Kotlin`: `1.9.21` -> `1.9.22`
  * `MicroUtils`: `0.20.22` -> `0.20.31`
  * `KSLog`: `1.3.1` -> `1.3.2`
* `Core`:
  * Rename `CookiesNavigationConfigsRepo` to `StorageNavigationConfigsRepo`
  * Add opportunity to set the `Storage` in `StorageNavigationConfigsRepo`
  * New configs repo `UrlParametersNavigationConfigsRepo`

## 0.3.8

* `Core`:
  * Fixes in saving of hierarchy: now changes in configs taking in care

## 0.3.7

* `Core`:
  * All callbacks in `onChangesInSubTree`/`onChangesInSubChains`/`onChangesInSubNodes` suspendable

## 0.3.6

* `Versions`:
  * `Serialization`: `1.6.1` -> `1.6.2`
  * `MicroUtils`: `0.20.15` -> `0.20.22`
* `Core`:
  * Add opportunity to use `onChangesInSubTree`/`onChangesInSubChains`/`onChangesInSubNodes`
  * Rewrite saving algorithm basing 

## 0.3.5

* `Core`:
  * A lot of different urgent fixes in `Chain`/`Node` flow extensions
    * Also added `fun`ctional variants of flow getter which allow to setup initial value of result
    flow

## 0.3.4

* `Core`:
  * `Common`:
    * Fixes in hierarchy saver/loader
    * All `restoreHierarchy` extensions now not suspendable
  * `JS`:
    * Experimentally added `OneParameterUrlNavigationConfigsRepo`

## 0.3.3

* `MVVM`:
  * New `ViewFragment#onSetNode` hook

## 0.3.2

* `Versions`:
  * `Kotlin`: `1.9.20` -> `1.9.21`
  * `Serialization`: `1.6.0` -> `1.6.1`
  * `MicroUtils`: `0.20.12` -> `0.20.15`
  * `KSLog`: `1.2.4` -> `1.3.1`
  * `UUID`: `0.8.1` -> `0.8.2`
  * `Compose`: `1.5.10` -> `1.5.11`

## 0.3.1

**This update contains upgrade up to gradle 8+. Be careful during to use of this library**

**This update have set up `compatibility` and `target` JDK versions to 17**

* `Versions`:
  * `Kotlin`: `1.9.0` -> `1.9.20`
  * `MicroUtils`: `0.20.0` -> `0.20.11`
  * `KSLog`: `1.2.0` -> `1.2.3`
  * `Koin`: `3.4.3` -> `3.5.0`
  * `Compose`: `1.5.1` -> `1.5.10`

## 0.3.0

* `Versions`:
    * `Kotlin`: `1.8.22` -> `1.9.0`
    * `MicroUtils`: `0.19.8` -> `0.20.0`
    * `KSLog`: `1.1.1` -> `1.2.0`
    * `UUID`: `0.7.1` -> `0.8.0`
    * `Koin`: `3.4.2` -> `3.4.3`

## 0.2.5

* `Versions`:
  * `Kotlin`: `1.8.22`
  * `Coroutines`: `1.7.3`
  * `MicroUtils`: `0.19.8`
  * `Compose`: `1.4.3`

## 0.2.4

* Fixes in part of non-storable nodes in hierarchy saving

## 0.2.3

* `Versions`:
  * `Coroutines`: `1.7.2`
  * `MicroUtils`: `0.19.7`
  * `Compose`: `1.4.1`
* Fix in `NavigationChain` saving

## 0.2.2

* `Versions`:
  * `Kotlin`: `1.8.20`
  * `Serialization`: `1.5.1`
  * `MicroUtils`: `0.19.5`
  * `UUID`: `0.7.1`
* `MVVM`:
  * Add `NavigationMVVMSingleActivity` as abstract class for single-activity oriented way of navigation
  * All `initNavigation` variants uses coroutine scope with supervisor job to avoid redundant navigation fails
    * Add `NavigationFragmentInfoProvider`. Realizations of `NavigationFragmentInfoProvider` can be used for passing in
      a list into `initNavigation` function for automatization of its parts registration

## 0.2.1

* Fixes in updates listening of chains and nodes

## 0.2.0

* `Versions`:
  * `MicroUtils`: `0.17.6` -> `0.18.3`
* Get back `mvvm` module with filled abstractions
* Improve Sample

## 0.1.0

* Fix of bug in start of chain
* `Versions`:
  * `Kotlin`: `1.8.20`
  * `MicroUtils`: `0.17.6`
  * `KSLog`: `1.1.1`

## 0.0.19

**THIS UPDATE CONTAINS DEPRECATIONS REMOVING**

* Fixes for android
* Add find extensions for chains and nodes

## 0.0.18

* `NavigationChain.parentNode` now is public
* Improvements in CRUD operations in navigation tree
* Improvements in replacing of nodes and chain diffs handlings

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
