(ns guestbook.messages
  (:require [guestbook.db.core :as db]
            [guestbook.validation :refer [validate-message]]))

(defn message-list []
  (do
    (println "In message-list")
    (try
      (let [m (db/get-messages)]
        (println m)
        {:messages (vec m)})
      (catch Exception e (str "caught exception: " (.getMessage e)))
      (finally (prn "final exception.")))))

(defn save-message! [message]
  (if-let [errors (validate-message message)]
    (throw (ex-info "Message is invalid"
                    {:guestbook/error-id :validation
                     :errors errors}))
    (db/save-message! message)))
