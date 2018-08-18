(ns clara-eav.test-runner
  (:require [doo.runner :refer-macros [doo-tests]]
            [clara-eav.eav-test]
            [clara-eav.store-test]
            [clara-eav.session-test]
            [clara-eav.rules-test]))

(doo-tests 'clara-eav.eav-test
           'clara-eav.store-test
           'clara-eav.session-test
           'clara-eav.rules-test)
