(ns demo.goldly.persist-tds
  (:require
   [taoensso.timbre :refer [debug info]]
   [clojure.java.io :as io]
   [tablecloth.api :as tc]
   [tech.v3.io :as techio]
   [ring.util.response :as res]
   [ring.util.io :as ring-io]
   [tech.v3.libs.arrow :as arrow]
   [reval.persist.protocol :refer [save loadr]]))

; csv

(defmethod save :ds-csv [_ file-name ds]
  (info "saving csv file: " file-name)
  (tc/write! ds file-name)
  ds ; important to be here, as save-study is used often in a threading macro
  )

; nippy

(defmethod save :nippy-gz [_ file-name ds]
  (let [s (techio/gzip-output-stream! file-name)]
    (info "saving nippy file " file-name " count: " (tc/row-count ds))
    (techio/put-nippy! s ds)
    ds ; important to be here, as save-study is used often in a threading macro
    ))

(defmethod loadr :nippy-gz [_ file-name]
  (let [s (techio/gzip-input-stream file-name)
        ds (techio/get-nippy s)]
    (debug "loaded ds " name " count: " (tc/row-count ds))
    ds))

; arrow

; https://github.com/apache/arrow

(defmethod save :arrow [_ file-name ds]
  (info "saving arrow file " file-name " count: " (tc/row-count ds))
  (try
    (arrow/write-dataset-to-stream! ds file-name {})
    (catch Exception x
      (println "EXCEPTION!" x)))
  ds ; important to be here, as save-study is used often in a threading macro
  )

(defmethod loadr :arrow [_ file-name]
  (arrow/read-stream-dataset-copying file-name))

(defn ds->response-arrow [ds]
  (res/response
   (ring-io/piped-input-stream
    (fn [ostream]
      (arrow/write-dataset-to-stream! ds ostream {})))))

#_(defn filename->response-arrow [filename]
    (-> filename
        load-arrow
        ds->response-arrow))

(comment

  (with-open [o (io/output-stream "/tmp/XXX.xxx")]
    (.write o 65))

  (-> {:x [1 2 3]
       :y ["A" "B" "A"]}
      tc/dataset
      (arrow/write-dataset-to-stream! "/tmp/data.arrow" {}))

  ; java.lang.NoClassDefFoundError: Could not initialize class org.apache.arrow.memory.UnsafeAllocationManager

  ; (save-arrow "/tmp/ZZZZ.arrow" (tc/dataset {:a [1.0 2.0 3.4]}))

 ;
  )

