rem hardwired to run tcpinterceptor listening on port 8000, and forwarding to localhost:8080
java -classpath .\classes org.apache.axis.utils.TCPInterceptor 8000 localhost 8080
