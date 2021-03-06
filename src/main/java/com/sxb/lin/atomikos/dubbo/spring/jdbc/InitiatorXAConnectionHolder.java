package com.sxb.lin.atomikos.dubbo.spring.jdbc;

import java.sql.SQLException;
import java.sql.Savepoint;

import javax.sql.XAConnection;
import javax.transaction.xa.XAResource;

import org.springframework.jdbc.datasource.ConnectionHolder;
import org.springframework.transaction.NestedTransactionNotSupportedException;

import com.atomikos.datasource.TransactionalResource;
import com.atomikos.datasource.xa.XAResourceTransaction;
import com.atomikos.icatch.CompositeTransaction;
import com.atomikos.icatch.CompositeTransactionManager;
import com.atomikos.icatch.config.Configuration;
import com.sxb.lin.atomikos.dubbo.DubboXATransactionalResource;
import com.sxb.lin.atomikos.dubbo.service.DubboTransactionManagerService;
import com.sxb.lin.atomikos.dubbo.service.DubboTransactionManagerServiceProxy;

public class InitiatorXAConnectionHolder extends ConnectionHolder{
	
	private XAConnection xaConnection;
	
	private XAResource xaResource;
	
	private XAResourceTransaction xaResourceTransaction;

	public InitiatorXAConnectionHolder(XAConnection xaConnection) 
			throws SQLException {
		super(xaConnection.getConnection());
		this.xaConnection = xaConnection;
		this.xaResource = xaConnection.getXAResource();
	}

	public XAConnection getXaConnection() {
		return xaConnection;
	}
	
	public XAResource getXaResource() {
		return xaResource;
	}
	
	public void start(){
		this.setTransactionActive(true);
		CompositeTransactionManager compositeTransactionManager = Configuration.getCompositeTransactionManager();
		CompositeTransaction compositeTransaction = compositeTransactionManager.getCompositeTransaction();
		long startTime = System.currentTimeMillis();
		long timeout = compositeTransaction.getTimeout() + DubboTransactionManagerService.ADD_TIME;
		DubboTransactionManagerServiceProxy instance = DubboTransactionManagerServiceProxy.getInstance();
		DubboXATransactionalResource dubboXATransactionalResource = instance.getDubboXATransactionalResource();
		TransactionalResource res = 
				dubboXATransactionalResource.createTransactionalResource(instance.getFirstUniqueResourceName(), startTime + timeout);
		XAResourceTransaction restx = (XAResourceTransaction) res.getResourceTransaction(compositeTransaction);
		restx.setXAResource(xaResource);
		restx.resume();
		this.xaResourceTransaction = restx;
	}

	public void end(){
		xaResourceTransaction.suspend();
	}
	
	@Override
	public boolean hasConnection() {
		return (this.xaConnection != null);
	}

	@Override
	public boolean supportsSavepoints() throws SQLException {
		return false;
	}

	@Override
	public Savepoint createSavepoint() throws SQLException {
		throw new NestedTransactionNotSupportedException(
				"Cannot create a nested transaction because savepoints are not supported by your JDBC driver");
	}

	@Override
	public void reset() {
		super.reset();
		this.xaConnection = null;
		this.xaResource = null;
		this.xaResourceTransaction = null;
	}
}
