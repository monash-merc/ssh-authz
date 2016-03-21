<html lang="en" ng-app="OAuthApp">
<head>
    <link rel="stylesheet"
          href="https://ajax.googleapis.com/ajax/libs/angular_material/0.10.0/angular-material.min.css">
    <link rel="stylesheet" href="https://fonts.googleapis.com/css?family=RobotoDraft:300,400,500,700,400italic">
    <meta name="viewport" content="initial-scale=1"/>
</head>
<body layout="column" ng-controller="AppCtrl">
<md-toolbar class="md-theme-light">
    <h1 class="md-toolbar-tools">
        <span>Server error</span>
    </h1>
</md-toolbar>


<md-content layout-padding style="padding: 24px;">
    <h2 class="md-title">Your request could not be completed.</h2>
    <md-card>
        <md-card-content>
            Something went wrong when processing your request (status code ${status}). Try again later.
        </md-card-content>
    </md-card>

</md-content>

<!-- Angular Material Dependencies -->
<script src="https://ajax.googleapis.com/ajax/libs/angularjs/1.4.2/angular.min.js"></script>
<script src="https://ajax.googleapis.com/ajax/libs/angularjs/1.4.2/angular-animate.min.js"></script>
<script src="https://ajax.googleapis.com/ajax/libs/angularjs/1.4.2/angular-aria.min.js"></script>
<script src="https://ajax.googleapis.com/ajax/libs/angular_material/0.10.0/angular-material.min.js"></script>
<script src="static/app.js"></script>
</body>
</html>
