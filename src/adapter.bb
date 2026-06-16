(ns adapter
  (:require
   [clojure.java.io :as io]
   [clojure.string :as str]))

(defn env [k]
  (System/getenv k))

(defn request-body []
  (let [len (some-> (env "CONTENT_LENGTH") parse-long)]
    (when (and len (pos? len))
      (let [buf (char-array len)]
        (.read *in* buf 0 len)
        (String. buf)))))

(defn cgi-headers []
  (merge
    (into {}
          (for [[k v] (System/getenv)
                :when (str/starts-with? k "HTTP_")]
            [(-> k
                 (subs 5)
                 str/lower-case
                 (str/replace "_" "-"))
             v]))
    (cond-> {}
      (System/getenv "CONTENT_TYPE")
      (assoc "content-type"
             (System/getenv "CONTENT_TYPE"))

      (System/getenv "CONTENT_LENGTH")
      (assoc "content-length"
             (System/getenv "CONTENT_LENGTH")))))

(def req
  {:server-port      (some-> (env "SERVER_PORT") parse-long)
   :server-name      (env "SERVER_NAME")
   :remote-addr      (env "REMOTE_ADDR")
   :uri              (or (env "PATH_INFO") "/")
   :query-string     (env "QUERY_STRING")
   :scheme           (if (= "on" (env "HTTPS")) :https :http)
   :request-method   (some-> (env "REQUEST_METHOD")
                         str/lower-case
                         keyword)
   :content-type     (env "CONTENT_TYPE")
   :content-length   (some-> (env "CONTENT_LENGTH") parse-long)
   :headers          (cgi-headers)
   :body             (request-body)})

(defn send-response [{:keys [status headers body]}]
  ;; Status header (optional)
  (when (and status (not= status 200))
    (println (str "Status: " status)))

  ;; Headers
  (doseq [[k v] headers]
    (println (str k ": " v)))

  ;; Required blank line
  (println)

  ;; Body
  (cond
    (string? body)
    (print body)

    (instance? java.io.InputStream body)
    (io/copy body System/out)

    (sequential? body)
    (doseq [chunk body]
      (print chunk))

    :else
    (print (str body))))

