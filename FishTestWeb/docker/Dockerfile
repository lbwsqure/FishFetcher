# Use Tomcat 11, which supports Servlets 5
FROM tomcat:11.0.0-M24-jdk21-temurin-noble

# This limits the amount of memory used
ENV JAVA_OPTS="-Xmx300m"

# Expose port 8080 when running on localhost
EXPOSE 8080

# Copy in our ROOT.war to the right place in the container
COPY ROOT.war /usr/local/tomcat/webapps/

# LOCALHOST:  Run catalina in the container
# Should map localhost:8080 to this app
CMD ["catalina.sh", "run"]