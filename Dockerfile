FROM openjdk:8
VOLUME /tmp
EXPOSE 9003
ADD target/sales-service-1.0.jar sales-service-1.0.jar 
ENTRYPOINT ["java","-jar","/sales-service-1.0.jar"]