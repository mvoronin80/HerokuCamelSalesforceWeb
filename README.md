Demonstration project for DreamForce'17. Apache Camel based Salesforce integration on Heroku platform.
This project is worker part (catching notifications from salesforce and update user's Telegram chat).
Installation steps:
0. FOLLOW STEPS for WORKER project first (https://github.com/mvoronin80/HerokuCamelSalesforceWorker see ReadMe file)
1. In source code:
	1.1. Set telegram api token .to("telegram:bots/{your token here}")
2. Create app in heroku and deploy source code to heroku (REMEMBER app name)
3. Create remote site settings in Salesforce (if your heroku app name is "immerse-falls-12345" then remote site is "http://immerse-falls-12345.herokuapp.com"). Disable protocol security for remote site.
4. Change HerokuNotifier Apex class: request.setEndpoint('http://{your heroku app name}.herokuapp.com/camel/cases')
5. Save changes to Git, deploy and scale to heroku

