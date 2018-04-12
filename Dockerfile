FROM clojure
MAINTAINER Chris Rink <chrisrink10@gmail.com>

RUN mkdir -p /usr/src/app
WORKDIR /usr/src/app

COPY project.clj /usr/src/app/
RUN lein deps

COPY . /usr/src/app
RUN mv "$(lein uberjar | sed -n 's/^Created \(.*standalone\.jar\)/\1/p')" repopreview.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "repopreview.jar"]
CMD ["start"]