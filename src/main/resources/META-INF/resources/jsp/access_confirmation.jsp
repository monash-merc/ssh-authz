<html lang="en" ng-app="OAuthApp">
  <head>
    <link rel="stylesheet" href="https://ajax.googleapis.com/ajax/libs/angular_material/0.10.0/angular-material.min.css">
    <link rel="stylesheet" href="https://fonts.googleapis.com/css?family=RobotoDraft:300,400,500,700,400italic">
    <meta name="viewport" content="initial-scale=1" />
  </head>
  <body layout="column" ng-controller="AppCtrl">
    <md-toolbar class="md-theme-light">
        <h1 class="md-toolbar-tools">
          <span>Authorisation required</span>
        </h1>
     </md-toolbar>
     <authz:authorize ifAllGranted="ROLE_USER">

     <script type="text/javascript">
      var oauthScopes = ${json_scopes};
     </script>

      <md-content layout-padding style="padding: 24px;">
        <h2 class="md-title"><strong>${client.clientId}</strong> is requesting permission to act on your behalf</h2>
        <md-card>
            <md-card-content>
                <strong>${client.clientId}</strong> would like to:
                <ul>
                    <li>Access your MASSIVE and/or CVL account</li>
                </ul>
            </md-card-content>
        </md-card>
        <section layout="row" layout-sm="column" layout-align="center center">
            <form id='confirmationForm' name='confirmationForm' action='authorize' method='post' ng-init='hpcAccountAccess = false;'>
                <input name='user_oauth_approval' value='true' type='hidden'/>
                <input name='scope.HPC_ACCOUNT_ACCESS' ng-value='hpcAccountAccess' type='hidden'>
                <md-button class="md-raised md-primary" ng-click="hpcAccountAccess = true;">Sounds good to me</md-button>
                <md-button class="md-raised md-warn" ng-click="hpcAccountAccess = false;">No thanks</md-button>
            </form>
        </section>

      </md-content>
     </authz:authorize>

    <!-- Angular Material Dependencies -->
    <script src="https://ajax.googleapis.com/ajax/libs/angularjs/1.4.2/angular.min.js"></script>
    <script src="https://ajax.googleapis.com/ajax/libs/angularjs/1.4.2/angular-animate.min.js"></script>
    <script src="https://ajax.googleapis.com/ajax/libs/angularjs/1.4.2/angular-aria.min.js"></script>
    <script src="https://ajax.googleapis.com/ajax/libs/angular_material/0.10.0/angular-material.min.js"></script>
    <script src="static/app.js"></script>
  </body>
</html>