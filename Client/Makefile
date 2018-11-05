all: java.policy compile-client

java.policy: FORCE
	@echo "Creating client java policy"
	@echo "grant codebase \"file:`pwd`/\" {" > java.policy
	@echo "permission java.security.AllPermission;" >> java.policy
	@echo "};" >> java.policy

FORCE: ;

../Server/RMIInterfaceMiddle.jar: ../Server/MiddlewareServer/Interface/IMiddleware.java
	make -C ../Server/ RMIInterfaceMiddle.jar

compile-client: ../Server/RMIInterfaceMiddle.jar
	javac -cp ../Server/RMIInterfaceMiddle.jar Client/*.java

clean:
	rm -f Client/*.class
	rm -f ../Server/RMIInterfaceMiddle.jar
	rm -f java.policy
