(set-env!
 :source-paths #{"src"}
 :resource-paths #{"res"}
 :dependencies '[[org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.9.495" :scope "test"]

                 [aysylu/loom "1.0.0"]
                 [org.clojure/data.json "0.2.6"]
                 [org.clojure/test.check "0.9.0" :scope "test"]
                 [tolitius/boot-check "0.1.4"]

                 ;; cljs
                 [adzerk/boot-cljs "2.1.1"]
                 [pandeiro/boot-http "0.7.6"]
                 [org.clojure/tools.nrepl "0.2.12"]
                 [adzerk/boot-reload "0.5.1"]
                 [adzerk/boot-cljs-repl "0.3.3"]
                 [com.cemerick/piggieback "0.2.1"]
                 [weasel "0.7.0"]

                 [cljsjs/cytoscape "3.1.4-0"]])

(require '[clojure.java.io :as io]
         '[metro.file]
         '[clojure.data.json :as json]
         '[tolitius.boot-check :as check]

         '[adzerk.boot-cljs :refer [cljs]]
         '[pandeiro.boot-http :refer [serve]]
         '[adzerk.boot-reload :refer [reload]]
         '[adzerk.boot-cljs-repl :refer [cljs-repl start-repl]])


(deftask dev
  []
  (comp
   (serve :dir "target")
   (watch)
   (reload)
   (cljs-repl)
   (cljs)
   (target :dir #{"target"})))

(deftask raw-file-to-json
  [c city CITY str "The city with the subways"]
  (with-pre-wrap fileset
    (with-open [w (io/writer (str "res/" city ".json"))]
      (.write w (json/json-str
                 (metro.file/format-stations city))))
    fileset))

(deftask check-sources []
  (set-env! :source-paths #{"src"})
  (comp
    (check/with-yagni)
    (check/with-eastwood)
    (check/with-kibit)
    (check/with-bikeshed)))

(boot.core/load-data-readers!) 
