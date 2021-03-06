package com.freelycar.shiro;


import com.freelycar.entity.Admin;
import com.freelycar.entity.Permission;
import com.freelycar.entity.Role;
import com.freelycar.service.impl.AdminServiceImpl;
import com.freelycar.service.impl.RoleServiceImpl;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.shiro.authc.*;
import org.apache.shiro.authz.AuthorizationException;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.realm.jdbc.JdbcRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashSet;
import java.util.Set;

public class ShiroRealmBaseDatabase extends JdbcRealm {

	@Autowired
	private AdminServiceImpl adminService;
	@Autowired
	private RoleServiceImpl roleService;

	private static final Logger log = LogManager.getLogger(ShiroRealmBaseDatabase.class);
	
	@Override
	protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
		UsernamePasswordToken upToken = (UsernamePasswordToken) token;
        String username = upToken.getUsername();
        if (username == null) {
            throw new AccountException("Null usernames are not allowed by this realm.");
        }
        SimpleAuthenticationInfo info = null;
        Admin admin = adminService.findAdminByAccount(username);
        if (admin == null) {
        	throw new UnknownAccountException("No account found for user [" + username + "]");
        }
        if(!admin.getCurrent()){
        	final String message = "Account [" + username + "] is not available.";
        	log.debug(message);
        	throw new LockedAccountException(message);
        }
        String password = admin.getPassword();
        info = new SimpleAuthenticationInfo(username, password.toCharArray(), getName());
		return info;
	}

	@Override
	protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {
		if (principals == null) {
            throw new AuthorizationException("PrincipalCollection method argument cannot be null.");
        }

        String username = (String) getAvailablePrincipal(principals);

        Set<String> roleNames = new HashSet<>();
        Set<String> permissions = new HashSet<>();
        Admin admin = adminService.findAdminByAccount(username);
        if (admin == null) {
        	throw new UnknownAccountException("No account found for user [" + username + "]");
        }
        if(!admin.getCurrent()){
        	final String message = "Account [" + username + "] is not available.";
        	log.debug(message);
        	throw new LockedAccountException(message);
        }
        Role role = roleService.get(admin.getRoleId());
        roleNames.add(role.getRoleName());
        if(this.permissionsLookupEnabled){
        	for(Permission p : role.getPermissions()){
        		permissions.add(p.getPermission());
        	}
        }
        SimpleAuthorizationInfo info = new SimpleAuthorizationInfo(roleNames);
        info.setStringPermissions(permissions);
		return info;
	}
	
}
