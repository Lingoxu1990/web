/**
 * 用户权限判断，在router中使用`resolve`属性来调用。
 * 目前未接入。
 * 需求：判断某个请求需要服务端认证权限规则时可接入
 */
myApp.factory('authorizationService', function ($resource, $q, $rootScope, $location, ACCESS_LEVELS) {
    return {
        // 将权限缓存到 Session，以避免后续请求不停的访问服务器
        permissionModel: { permission: {}, isPermissionLoaded: false },
 
        permissionCheck: function (roleCollection) {
            // 返回一个承诺(promise).
            var deferred = $q.defer();
 
            // 这里只是在承诺的作用域中保存一个指向上层作用域的指针。
            var parentPointer = this;
 
            // 检查是否已从服务获取到权限对象(已登录用户的角色列表)
            if (this.permissionModel.isPermissionLoaded) {
 
                // 检查当前用户是否有权限访问当前路由
                this.getPermission(this.permissionModel, roleCollection, deferred);
            } else {
                // 如果还没权限对象，我们会去服务端获取。
                // 'api/permissionService' 是本例子中的 web 服务地址。
 
                $resource('/api/permissionService').get().$promise.then(function (response) {
                    log('去服务器获取用户权限', response);
                    // 当服务器返回之后，我们开始填充权限对象
                    parentPointer.permissionModel.permission = response;
 
                    // 将权限对象处理完成的标记设为 true 并保存在 Session，
                    // Session 中的用户，在后续的路由请求中可以重用该权限对象
                    parentPointer.permissionModel.isPermissionLoaded = true;
                    // 检查当前用户是否有必须角色访问该路由
                    parentPointer.getPermission(parentPointer.permissionModel, roleCollection, deferred);
                }
                );
            }
            return deferred.promise;
        },
 
        //方法:检查当前用户是否有必须角色访问该路由
        //'permissionModel' 保存了从服务端返回的当前用户的角色信息
        //'roleCollection' 保存了可访问当前路由的角色列表
        //'deferred' 是用来处理承诺的对象
        getPermission: function (permissionModel, roleCollection, deferred) {
            var ifPermissionPassed = false;
            log('roleCollection', roleCollection);
            angular.forEach(roleCollection, function (role) {
               
                switch (role) {
                    case ACCESS_LEVELS.superUser:
                        if (permissionModel.permission.isSuperUser) {
                            ifPermissionPassed = true;
                        }
                        break;
                    case ACCESS_LEVELS.admin:
                        if (permissionModel.permission.isAdministrator) {
                            ifPermissionPassed = true;
                        }
                        break;
                    case ACCESS_LEVELS.user:
                        if (permissionModel.permission.isUser) {
                            ifPermissionPassed = true;
                        }
                        break;
                    default:
                        ifPermissionPassed = false;
                }
            });
            if (!ifPermissionPassed) {
                // 如果用户没有必须的权限，我们把用户引导到无权访问页面
                // $location.path(routeForUnauthorizedAccess);
                $location.path('/index');
                // 由于这个处理会有延时，而这期间页面位置可能发生改变， 
                // 我们会一直监视 $locationChangeSuccess 事件
                // 并且当该事件发生的时，就把掉承诺解决掉。
                $rootScope.$on('$locationChangeSuccess', function (next, current) {
                    log('监听浏览器地址改变，改变了要记得告诉我噢。') // todo
                    deferred.resolve();
                });
            } else {
                deferred.resolve();
            }
        }
    };
});

