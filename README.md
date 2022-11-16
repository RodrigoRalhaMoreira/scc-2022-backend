# scc-2022-backend

To run the code you compile it:
mvn clean compile assembly:single

Then you deploy it:
az login
az account set --subscription "1d174378-ca44-4436-a490-f7f93363a27b"
mvn clean compile package azure-webapp:deploy

Then you can make the requests on postman

To run azure functions:
-> Change pom from <packaging>war<packaging> to <packaging>jar<packaging>
-> run: mvn clean compile package azure-functions:deploy

Developed by:

Rodrigo Moreira 57943 - rr.moreira@campus.fct.unl.pt

Dinis Silvestre 58763 - dj.silvestre@campus.fct.unl.pt 

Tiago Duarte 58125 - tj.duarte@campus.fct.unl.pt
