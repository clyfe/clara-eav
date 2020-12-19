(ns clara-eav.test-runner
  (:require [cljs.test :refer-macros [run-tests]]
            [clara-eav.eav-test]
            [clara-eav.store-test]
            [clara-eav.session-test]
            [clara-eav.rules-test]))

(run-tests 'clara-eav.eav-test
           'clara-eav.store-test
           'clara-eav.session-test
           'clara-eav.rules-test)
