(defproject cc.qbits/spex "0.7.1-SNAPSHOT"
  :description "Simple spex extensions, utils"
  :url "https://github.com/mpenet/spex"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.10.1"]]
  :profiles {:dev {:dependencies [[org.clojure/test.check "0.9.0"]
                                  [com.gfredericks/test.chuck "0.2.6"]
                                  [thheller/shadow-cljs "2.8.69"]]}}
  :source-paths ["src/clj"]
  :global-vars {*warn-on-reflection* true}
  :aliases {"shadow-cljs" ["run" "-m" "shadow.cljs.devtools.cli"]
            "test-cljs"   ["shadow-cljs" "compile" "test"]})
