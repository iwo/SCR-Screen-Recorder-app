/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Original file: aidl/ILicensingService.aidl
 */
package com.google.android.vending.licensing;
import java.lang.String;
import android.os.RemoteException;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Binder;
import android.os.Parcel;
public interface ILicensingService extends IInterface
{
/** Local-side IPC implementation stub class. */
public static abstract class Stub extends Binder implements ILicensingService
{
private static final String DESCRIPTOR = "com.android.vending.licensing.ILicensingService";
/** Construct the stub at attach it to the interface. */
public Stub()
{
this.attachInterface(this, DESCRIPTOR);
}
/**
 * Cast an IBinder object into an ILicensingService interface,
 * generating a proxy if needed.
 */
public static ILicensingService asInterface(IBinder obj)
{
if ((obj==null)) {
return null;
}
IInterface iin = (IInterface)obj.queryLocalInterface(DESCRIPTOR);
if (((iin!=null)&&(iin instanceof ILicensingService))) {
return ((ILicensingService)iin);
}
return new Proxy(obj);
}
public IBinder asBinder()
{
return this;
}
public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException
{
switch (code)
{
case INTERFACE_TRANSACTION:
{
reply.writeString(DESCRIPTOR);
return true;
}
case TRANSACTION_checkLicense:
{
data.enforceInterface(DESCRIPTOR);
long _arg0;
_arg0 = data.readLong();
String _arg1;
_arg1 = data.readString();
ILicenseResultListener _arg2;
_arg2 = ILicenseResultListener.Stub.asInterface(data.readStrongBinder());
this.checkLicense(_arg0, _arg1, _arg2);
return true;
}
}
return super.onTransact(code, data, reply, flags);
}
private static class Proxy implements ILicensingService
{
private IBinder mRemote;
Proxy(IBinder remote)
{
mRemote = remote;
}
public IBinder asBinder()
{
return mRemote;
}
public String getInterfaceDescriptor()
{
return DESCRIPTOR;
}
public void checkLicense(long nonce, String packageName, ILicenseResultListener listener) throws RemoteException
{
Parcel _data = Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeLong(nonce);
_data.writeString(packageName);
_data.writeStrongBinder((((listener!=null))?(listener.asBinder()):(null)));
mRemote.transact(Stub.TRANSACTION_checkLicense, _data, null, IBinder.FLAG_ONEWAY);
}
finally {
_data.recycle();
}
}
}
static final int TRANSACTION_checkLicense = (IBinder.FIRST_CALL_TRANSACTION + 0);
}
public void checkLicense(long nonce, String packageName, ILicenseResultListener listener) throws RemoteException;
}
