## Changelog

### [Unreleased]
#### Changed
- Fixed upsert eav with value nil
- Fixed rule and query eav with value nil
- Updated Clara-Rules dependency to 0.23.0 version.

### [0.1.7] - 2018-05-05
#### Changed
- Merged Guide into README.
- Renamed :db/id to :eav/eid.
- Updated Clara-Rules dependency to 0.19.1 version.
- Updated Clojure dependency to 1.10.0 version.
- Updated ancillary dependencies to recent versions.

### [0.1.6] - 2018-09-08
#### Changed
- Minor code refactor to rules namespace and test, and dsl test.
- Fixed grammar errors in the guide.
- Fixed rules specs and updated upsert docstring.
- Made clara-eav.eav namespace public.
- Minor docs rewording.
- Commands should be namespaced keywords, rules test now reflects that.
- Made dsl/transform private
#### Added
- Code coverage via lein-cloverage.
- MIT license badge.
- Project url.
- Support for tempids in transient eavs.
- Tests for tempids resolution being returned. 

### [0.1.5] - 2018-08-17
#### Removed
- Codox and Cljfmt from lein plugins list.
#### Added
- CircleCI integration.
- Missing 0.1.4 changelog entry.

### [0.1.4] - 2018-08-17
#### Added
- Spec instrumentation in tests.

### [0.1.3] - 2018-08-17
#### Added
- Changelog, regenerated docs.

### [0.1.2] - 2018-08-17
#### Changed
- Transients have an attribute `:eav/transient` instead `:transient`.

### [0.1.1] - 2018-08-17
#### Changed
- README.md rewording.

### 0.1.0 - 2018-08-17
#### Added
- First release.
