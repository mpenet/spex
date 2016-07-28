(defproject cc.qbits/spex "0.1.0"
  :description "Simple spex extensions, utils"
  :url "https://github.com/mpenet/spex"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0-alpha10"]
                 [org.clojure/test.check "0.9.0"]
                 ;; [com.gfredericks/test.chuck "0.2.6"]
                 ]
  :source-paths ["src/clj"]
  ;; :java-source-paths ["src/java"]
  ;; :javac-options ["-source" "1.6" "-target" "1.6" "-g"]
  :global-vars {*warn-on-reflection* true})
