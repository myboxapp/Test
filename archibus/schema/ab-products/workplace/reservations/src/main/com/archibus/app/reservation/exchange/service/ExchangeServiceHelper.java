package com.archibus.app.reservation.exchange.service;

import java.net.*;

import microsoft.exchange.webservices.data.*;

import com.archibus.app.reservation.domain.CalendarException;
import com.archibus.app.reservation.util.ICalendarSettings;  
import com.archibus.utility.*;

/**
 * Utility Class for Exchange Service configuration.
 * 
 * Managed in Spring.
 * 
 * @author Bart Vanderschoot
 * @since 21.2
 */
public class ExchangeServiceHelper implements ICalendarSettings {
    
    /** Error message indicating the connect failed because the URL is invalid. */
    // @translatable
    private static final String INVALID_EXCHANGE_URL = "Invalid Exchange URL [{0}]";

    /** Error message indicating the connect failed. */
    // @translatable
    private static final String CONNECT_FAILED =
            "Could not connect to Exchange. Please refer to archibus.log for details";
     
    /** The url. */
    private String url;
    
    /** The user name. */
    private String userName;
    
    /** The password. */
    private String password;
    
    /** the network domain. */
    private String domain;
    
    /** The proxy server. */
    private String proxyServer;
    
    /** The proxy port. */
    private Integer proxyPort;
    
    /** The version of Exchange. */
    private String version;
    
    /** The resource mailbox. */
    private String resourceAccount;
    
    /** The organizer mailbox used to create meetings for non-Exchange users. */
    private String organizerAccount;

    /**
     * Get an Exchange service instance for accessing the given mailbox.
     * 
     * @param email the mailbox to access
     * @return the exchange service
     */
    public ExchangeService initializeService(final String email) {
        try {
            ExchangeService exchangeService = this.getService(email);
            
            try {
                Folder.bind(exchangeService, WellKnownFolderName.Calendar, PropertySet.IdOnly);
            } catch (final ServiceResponseException exception) {
                /*
                 * Check whether the cause is a non-existant mailbox. In that case switch to the
                 * organizer mailbox for non-Exchange users (if this mailbox is defined).
                 */
                if (ServiceError.ErrorNonExistentMailbox.equals(exception.getErrorCode())) {
                    if (StringUtil.isNullOrEmpty(this.getOrganizerAccount())) {
                        // No organizer account is defined, so report the error.
                        // @translatable
                        throw new CalendarException(
                            "Requestor [{0}] does not have a valid mailbox on this Exchange server",
                            exception, ExchangeServiceHelper.class, email);
                    } else {
                        // Use the organizer account for connecting to Exchange.
                        exchangeService = this.getService(this.getOrganizerAccount());
                    }
                } else {
                    throw new CalendarException(CONNECT_FAILED, exception, ExchangeServiceHelper.class);
                }
                // CHECKSTYLE:OFF : Suppress IllegalCatch warning. Justification: third-party API
                // method throws a checked Exception, which needs to be wrapped in ExceptionBase.
            } catch (final Exception exception) {
                // CHECKSTYLE:ON
                throw new CalendarException(CONNECT_FAILED, exception, ExchangeServiceHelper.class);
            }
            
            return exchangeService;
        } catch (final URISyntaxException exception) {
            throw new CalendarException(INVALID_EXCHANGE_URL, exception,
                ExchangeServiceHelper.class, this.url);
        }
    }
    
    /**
     * Get an Exchange service instance using only the service account, without impersonation.
     * 
     * @return the exchange service
     */
    public ExchangeService initializeService() {
        try {
            return this.getService();
        } catch (final URISyntaxException exception) {
            throw new CalendarException(INVALID_EXCHANGE_URL, exception,
                ExchangeServiceHelper.class, this.url);
        }
    }
    
    /**
     * getService create instance for Exchange Service.
     * 
     * Create a new EWS Exchange Service for the impersonated user. This class should be configured
     * as a Spring Bean:
     * <ul>
     * <li>url: url of the Exchange Web Service https://mail.&lt;domain&gt;.com/ews/Exchange.asmx</li>
     * <li></li>
     * <li></li>
     * </ul>
     * 
     * @param impersonatedUserEmail the email address that should be impersonated.
     * 
     * @return service EWS Exchange Service
     * 
     * @throws URISyntaxException URI exception
     */
    public ExchangeService getService(final String impersonatedUserEmail) throws URISyntaxException {
        final ExchangeService exchangeService = getService();
        
        exchangeService.setImpersonatedUserId(new ImpersonatedUserId(ConnectingIdType.SmtpAddress,
            impersonatedUserEmail));
        
        return exchangeService;
    }
    
    /**
     * Create instance of the Exchange Service without impersonation.
     * 
     * @return service EWS Exchange Service
     * 
     * @throws URISyntaxException URI exception
     */
    public ExchangeService getService() throws URISyntaxException {
        ExchangeVersion exchangeVersion;
        if (this.version == null) {
            // default value
            exchangeVersion = ExchangeVersion.Exchange2010_SP1;
        } else {
            exchangeVersion = ExchangeVersion.valueOf(this.version);
        }
        
        final ExchangeService exchangeService = new ExchangeService(exchangeVersion);
        exchangeService.setUrl(new URI(this.url));
        
        // TODO: use encryption for password
      //  final Decoder1 decoder = new Decoder1();
       // final String passwordDecrypted = decoder.decode(this.password);
        
        if (StringUtil.notNullOrEmpty(this.domain)) {
            exchangeService.setCredentials(new WebCredentials(this.userName, this.password,
                this.domain));
        } else {
            exchangeService.setCredentials(new WebCredentials(this.userName, this.password));
        }
        
        if (StringUtil.notNullOrEmpty(this.proxyServer) && this.proxyPort != 0) {
            final WebProxy proxy = new WebProxy(this.proxyServer, this.proxyPort);
            proxy.setCredentials(this.userName, this.password, this.domain);
            exchangeService.setWebProxy(proxy);
        }
        return exchangeService;
    }
    
    /**
     * Getter for the Exchange URL property.
     * 
     * @return the Exchange URL property.
     */
    public String getUrl() {
        return this.url;
    }
    
    /**
     * Setter for the Exchange URL property.
     * 
     * @param url the URL to set
     */
    
    public void setUrl(final String url) {
        this.url = url;
    }
    
    /**
     * Getter for the userName property.
     * 
     * @see userName
     * @return the userName property.
     */
    public String getUserName() {
        return this.userName;
    }
    
    /**
     * Setter for the userName property.
     * 
     * @see userName
     * @param userName the userName to set
     */
    
    public void setUserName(final String userName) {
        this.userName = userName;
    }
    
    /**
     * Getter for the password property.
     * 
     * @see password
     * @return the password property.
     */
    public String getPassword() {
        return this.password;
    }
    
    /**
     * Setter for the password property.
     * 
     * @see password
     * @param password the password to set
     */
    
    public void setPassword(final String password) {
        this.password = password;
    }
    
    /**
     * Getter for the domain property.
     * 
     * @see domain
     * @return the domain property.
     */
    public String getDomain() {
        return this.domain;
    }
    
    /**
     * Setter for the domain property.
     * 
     * @see domain
     * @param domain the domain to set
     */
    
    public void setDomain(final String domain) {
        this.domain = domain;
    }
    
    /**
     * Getter for the proxyServer property.
     * 
     * @see proxyServer
     * @return the proxyServer property.
     */
    public String getProxyServer() {
        return this.proxyServer;
    }
    
    /**
     * Setter for the proxyServer property.
     * 
     * @see proxyServer
     * @param proxyServer the proxyServer to set
     */
    
    public void setProxyServer(final String proxyServer) {
        this.proxyServer = proxyServer;
    }
    
    /**
     * Getter for the proxyPort property.
     * 
     * @see proxyPort
     * @return the proxyPort property.
     */
    public Integer getProxyPort() {
        return this.proxyPort;
    }
    
    /**
     * Setter for the proxyPort property.
     * 
     * @see proxyPort
     * @param proxyPort the proxyPort to set
     */
    
    public void setProxyPort(final Integer proxyPort) {
        this.proxyPort = proxyPort;
    }
    
    /**
     * Getter for the version property.
     * 
     * @see version
     * @return the version property.
     */
    public String getVersion() {
        return this.version;
    }
    
    /**
     * Setter for the version property.
     * 
     * @see version
     * @param version the version to set
     */
    
    public void setVersion(final String version) {
        this.version = version;
    }
    
    /**
     * Get the email address of the resource account.
     * 
     * @return resource account email address
     */
    public String getResourceAccount() {
        return this.resourceAccount;
    }
    
    /**
     * Set the resource account email address.
     * 
     * @param resourceAccount the new resource account email address
     */
    public void setResourceAccount(final String resourceAccount) {
        this.resourceAccount = resourceAccount;
    }
    
    /**
     * Get the email address of the organizer account.
     * 
     * @return organizer account email address
     */
    public String getOrganizerAccount() {
        return this.organizerAccount;
    }
    
    /**
     * Set the organizer account email address.
     * 
     * @param organizerAccount the new organizer account email address
     */
    public void setOrganizerAccount(final String organizerAccount) {
        this.organizerAccount = organizerAccount;
    }

}