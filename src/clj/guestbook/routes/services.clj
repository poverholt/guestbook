(ns guestbook.routes.services
  (:require
   [guestbook.messages :as msg]
   [guestbook.middleware :as middleware]
   [guestbook.middleware.formats :as formats]
   [reitit.coercion.spec :as spec-coercion]
   [reitit.ring.coercion :as coercion]
   [reitit.ring.middleware.exception :as exception]
   [reitit.ring.middleware.multipart :as multipart]
   [reitit.ring.middleware.muuntaja :as muuntaja]
   [reitit.ring.middleware.parameters :as parameters]
   [reitit.swagger :as swagger]
   [reitit.swagger-ui :as swagger-ui]
   [ring.util.http-response :as response]))

(defn service-routes []
  ["/api" {:middleware
           [parameters/parameters-middleware ; query-params & form-params
            muuntaja/format-negotiate-middleware ; content-negotiation
            muuntaja/format-response-middleware ; encoding response body
            exception/exception-middleware ; exception handling
            muuntaja/format-request-middleware ; decoding request body
            coercion/coerce-response-middleware ; coercing response bodies
            coercion/coerce-request-middleware ; coercing request parameters
            multipart/multipart-middleware] ; multipart params
           :muuntaja formats/instance
           :coercion spec-coercion/coercion
           :swagger {:id ::api}}
   ["" {:no-doc true}
    ["/swagger.json" {:get (swagger/create-swagger-handler)}]
    ["/swagger-ui*" {:get (swagger-ui/create-swagger-ui-handler {:url "/api/swagger.json"})}]]
   ["/messages" {:get
                 {:responses {200
                              {:body ; Data Spec for response body
                               {:messages
                                [{:id pos-int?
                                  :name string?
                                  :messages string?
                                  :timestamp inst?}]}}}
                  :handler (fn [_] (response/ok (msg/message-list)))}}]
   ["/message" {:post {:parameters {:body ; Data Spec for Request body parameters
                                    {:name string?
                                     :message string?}}
                       :responses {200 {:body map?}
                                   500 {:errors map?}}
                       :handler (fn [{{params :body} :parameters}]
                                  (try
                                    (msg/save-message! params)
                                    (response/ok {:status :ok})
                                    (catch Exception e
                                      (let [{id :guestbook/error-id
                                             errors :errors}
                                            (ex-data e)]
                                        (case id
                                          :validation (response/bad-request {:errors errors})
                                          (response/internal-server-error {:errors {:server-error ["Failed to save message!"]}}))))))}}]])

