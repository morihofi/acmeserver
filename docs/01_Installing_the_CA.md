# 1. Installing the CA on your device or browser

First you need to download the root certificate (CA) that your ACME server instance uses.
You can download it from your running ACME server at https://acme.example.com/ca.crt (use the actual URL to your server)

## Browsers

### Firefox
1. Open Settings, search for "certificates". 
2. Then click on "Show certificates" and "Import". 
3. Select you certificate.
4. Set the checkmark, that you'll trust this certificate to identify websites
5. (Sometimes you need to restart Firefox)
6. That's all!

## Operating Systems

### Debian based (Ubuntu, Linux Mint, Proxmox, ...)
You just need to copy your CA certificate into the global root certificate store and update the local certificate caches. (You need `root` permissions)
```bash
cp ca.crt /usr/local/share/ca-certificates/acme-ca.crt
update-ca-certificates
```
You should see an output like this:
```
Updating certificates in /etc/ssl/certs...
1 added, 0 removed; done.
Running hooks in /etc/ca-certificates/update.d...

done.
```
That's all! 

### Windows
1. Access Certificate Management:
   - Click on the Start menu or press the Win key.
   - Type Manage computer certificates and press Enter.
   - This opens the Certificate Management tool for the local computer.

2. Install the CA Certificate:
   - In the Certificate Management window, navigate to Trusted Root Certification Authorities > Certificates.
   - Right-click on Certificates under Trusted Root Certification Authorities.
   - Select All Tasks > Import to start the Certificate Import Wizard.
   - Click Next to proceed in the wizard.
   - Use the Browse function to find and select your downloaded CA certificate file.
   - Once selected, click Next.
   - Ensure the certificate is placed in the Trusted Root Certification Authorities store.
   - Continue and finish the wizard by clicking Next and then Finish.

3. Verify the Installation:
   - The certificate should now be visible in the list under Trusted Root Certification Authorities.
   - Double-click on the certificate to open and verify its details.

4. Restart Your Computer:
   - For the changes to take full effect, restart your computer.


**You have successfully installed your ACME Server Root Certificate!
If you want to configure an ACME Client, you can now proceed with step 2.
Otherwise, you have nothing more to do.**
