FROM openjdk:17-slim
COPY sb-workflow-handler-1.0.0.jar /opt/
EXPOSE 9060
CMD ["/bin/bash", "-c", "java -XX:+PrintFlagsFinal $JAVA_OPTIONS -XX:+UnlockExperimentalVMOptions -jar /opt/sb-workflow-handler-1.0.0.jar"]
