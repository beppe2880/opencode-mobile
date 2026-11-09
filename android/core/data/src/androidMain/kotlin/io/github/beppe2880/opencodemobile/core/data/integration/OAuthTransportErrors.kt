package io.github.beppe2880.opencodemobile.core.data.integration

import io.github.beppe2880.opencodemobile.core.domain.ProviderOAuthTransport
import java.io.InterruptedIOException
import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.ProtocolException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.security.cert.CertPathValidatorException
import java.security.cert.CertificateException
import javax.net.ssl.SSLException
import javax.net.ssl.SSLPeerUnverifiedException

/** Inspect exception TYPES only; messages can contain URLs, authorization codes or host names. */
internal fun oauthTransportKind(error: Throwable): ProviderOAuthTransport {
    val chain = generateSequence(error) { it.cause }.take(8).toList()
    return when {
        chain.any { it is CertificateException || it is CertPathValidatorException || it is SSLPeerUnverifiedException } -> ProviderOAuthTransport.Certificate
        chain.any { it is UnknownHostException } -> ProviderOAuthTransport.DNS
        chain.any { it is SocketTimeoutException } -> ProviderOAuthTransport.Timeout
        chain.any { it is SSLException } -> ProviderOAuthTransport.TLS
        chain.any { it is ConnectException || it is NoRouteToHostException } -> ProviderOAuthTransport.Connect
        chain.any { it is ProtocolException } -> ProviderOAuthTransport.Protocol
        chain.any { it is InterruptedIOException } -> ProviderOAuthTransport.Interrupted
        chain.any { it is SocketException } -> ProviderOAuthTransport.ConnectionIO
        else -> ProviderOAuthTransport.IO
    }
}
