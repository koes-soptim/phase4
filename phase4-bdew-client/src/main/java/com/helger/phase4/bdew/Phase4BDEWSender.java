/*
 * Copyright (C) 2023 Gregor Scholtysik (www.soptim.de)
 * gregor[dot]scholtysik[at]soptim[dot]de
 *
 * Copyright (C) 2021 Philip Helger (www.helger.com)
 * philip[at]helger[dot]com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.helger.phase4.bdew;

import com.helger.phase4.attachment.AS4OutgoingAttachment;
import com.helger.phase4.attachment.WSS4JAttachment;
import com.helger.phase4.client.AS4ClientUserMessage;
import com.helger.phase4.crypto.ECryptoKeyEncryptionAlgorithm;
import com.helger.phase4.crypto.ECryptoKeyIdentifierType;
import com.helger.phase4.sender.AS4BidirectionalClientHelper;
import com.helger.phase4.sender.AbstractAS4UserMessageBuilder;
import com.helger.phase4.util.AS4ResourceHelper;
import com.helger.phase4.util.Phase4Exception;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.OverridingMethodsMustInvokeSuper;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.NotThreadSafe;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

/**
 * This class contains all the specifics to send AS4 messages with the BDEW
 * profile. See <code>sendAS4Message</code> as the main method to trigger the
 * sending, with all potential customization.
 *
 * @author Gregor Scholtysik
 */
@Immutable
public final class Phase4BDEWSender
{
  private static final Logger LOGGER = LoggerFactory.getLogger (Phase4BDEWSender.class);

  private Phase4BDEWSender()
  {}

  /**
   * @return Create a new Builder for AS4 messages if the payload is present.
   *         Never <code>null</code>.
   */
  @Nonnull
  public static BDEWUserMessageBuilder builder ()
  {
    return new BDEWUserMessageBuilder();
  }

  /**
   * Abstract BDEW UserMessage builder class with sanity methods
   *
   * @author Philip Helger
   * @param <IMPLTYPE>
   *        The implementation type
   */
  public abstract static class AbstractBDEWUserMessageBuilder<IMPLTYPE extends AbstractBDEWUserMessageBuilder<IMPLTYPE>>
                                                                extends
                                                                AbstractAS4UserMessageBuilder <IMPLTYPE>
  {
    public static final ECryptoKeyIdentifierType DEFAULT_KEY_IDENTIFIER_TYPE = ECryptoKeyIdentifierType.BST_DIRECT_REFERENCE;

    private ECryptoKeyIdentifierType m_eSigningKeyIdentifierType;
    private ECryptoKeyIdentifierType m_eEncryptionKeyIdentifierType;
    private AS4OutgoingAttachment m_aPayload;
    private BDEWPayloadParams m_aPayloadParams;

    protected AbstractBDEWUserMessageBuilder()
    {
      // Override default values
      try
      {
        httpClientFactory (new Phase4BDEWHttpClientSettings ());
        setSigningKeyIdentifierType (DEFAULT_KEY_IDENTIFIER_TYPE);
        encryptionKeyIdentifierType (DEFAULT_KEY_IDENTIFIER_TYPE);
      }
      catch (final Exception ex)
      {
        throw new IllegalStateException ("Failed to init AS4 Client builder", ex);
      }
    }

    /**
     * Encryption Key Identifier Type.
     *
     * @param eEncryptionKeyIdentifierType
     *        {@link ECryptoKeyIdentifierType}. Defaults to BST_DIRECT_REFERENCE. May
     *        be <code>null</code>.
     * @return this for chaining
     */
    @Nonnull
    public final IMPLTYPE encryptionKeyIdentifierType (@Nullable final ECryptoKeyIdentifierType eEncryptionKeyIdentifierType)
    {
      m_eEncryptionKeyIdentifierType = eEncryptionKeyIdentifierType;
      return thisAsT ();
    }

    /**
     * Signing Key Identifier Type.
     *
     * @param eSigningKeyIdentifierType
     *        {@link ECryptoKeyIdentifierType}. Defaults to BST_DIRECT_REFERENCE. May
     *        be <code>null</code>.
     * @return this for chaining
     */
    @Nonnull
    public final IMPLTYPE setSigningKeyIdentifierType (@Nullable final ECryptoKeyIdentifierType eSigningKeyIdentifierType)
    {
      m_eSigningKeyIdentifierType = eSigningKeyIdentifierType;
      return thisAsT ();
    }

    /**
     * Set the payload to be send out.
     *
     * @param aBuilder
     *        The payload builder to be used. GZip compression is automatically.
     *        enforced.
     * @param aPayloadParams
     *        The payload params to use. May be <code>null</code>.
     * @return this for chaining
     */
    @Nonnull
    public final IMPLTYPE payload (@Nullable final AS4OutgoingAttachment.Builder aBuilder,
                                   @Nullable final BDEWPayloadParams aPayloadParams)
    {
      m_aPayload = aBuilder != null ? aBuilder.compressionGZIP ().build () : null;
      m_aPayloadParams = aPayloadParams;
      return thisAsT ();
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public boolean isEveryRequiredFieldSet ()
    {
      if (!super.isEveryRequiredFieldSet ())
        return false;

      if (m_aPayload == null)
      {
        LOGGER.warn ("The field 'payload' is not set");
        return false;
      }

      return true;
    }

    @Override
    protected final void mainSendMessage () throws Phase4Exception
    {
      // Temporary file manager
      try (final AS4ResourceHelper aResHelper = new AS4ResourceHelper ())
      {
        // Start building AS4 User Message
        final AS4ClientUserMessage aUserMsg = new AS4ClientUserMessage (aResHelper);
        applyToUserMessage (aUserMsg);

        aUserMsg.cryptParams ().setKeyEncAlgorithm (ECryptoKeyEncryptionAlgorithm.ECDH_ES_KEYWRAP_AES_128);
        aUserMsg.cryptParams ().setKeyIdentifierType (m_eEncryptionKeyIdentifierType);
        aUserMsg.signingParams ().setKeyIdentifierType (m_eSigningKeyIdentifierType);
        // Empty string by purpose
        aUserMsg.setConversationID ("");

        // No payload - only one attachment
        aUserMsg.setPayload (null);

        // Add main attachment
        final WSS4JAttachment aPayloadAttachment = WSS4JAttachment.createOutgoingFileAttachment (m_aPayload,
                                                                                                 aResHelper);

        if (m_aPayloadParams != null)
        {
          if (m_aPayloadParams.getDocumentType () != null)
            aPayloadAttachment.customPartProperties ().put ("BDEWDocumentType", m_aPayloadParams.getDocumentType ());
          if (m_aPayloadParams.getDocumentDate () != null)
            aPayloadAttachment.customPartProperties ().put ("BDEWDocumentDate",
                    m_aPayloadParams.getDocumentDate ().withZoneSameInstant (ZoneOffset.UTC).toString ());
          if (m_aPayloadParams.getDocumentNumber () != null)
            aPayloadAttachment.customPartProperties ().put ("BDEWDocumentNo", String.valueOf (m_aPayloadParams.getDocumentNumber ()));
          if (m_aPayloadParams.getFulfillmentDate () != null)
            aPayloadAttachment.customPartProperties ().put ("BDEWFulfillmentDate",
                    m_aPayloadParams.getFulfillmentDate ().withZoneSameInstant (ZoneOffset.UTC).toString ());
          if (m_aPayloadParams.getSubjectPartyId () != null)
            aPayloadAttachment.customPartProperties ().put ("BDEWSubjectPartyID", m_aPayloadParams.getSubjectPartyId ());
          if (m_aPayloadParams.getSubjectPartyRole () != null)
            aPayloadAttachment.customPartProperties ().put ("BDEWSubjectPartyRole", m_aPayloadParams.getSubjectPartyRole ());
        }
        aUserMsg.addAttachment (aPayloadAttachment);

        // Add other attachments
        for (final AS4OutgoingAttachment aAttachment : m_aAttachments)
          aUserMsg.addAttachment (WSS4JAttachment.createOutgoingFileAttachment (aAttachment, aResHelper));

        // Main sending
        AS4BidirectionalClientHelper.sendAS4UserMessageAndReceiveAS4SignalMessage (m_aCryptoFactory,
                                                                                   pmodeResolver (),
                                                                                   incomingAttachmentFactory (),
                                                                                   incomingProfileSelector (),
                                                                                   aUserMsg,
                                                                                   m_aLocale,
                                                                                   m_sEndpointURL,
                                                                                   m_aBuildMessageCallback,
                                                                                   m_aOutgoingDumper,
                                                                                   m_aIncomingDumper,
                                                                                   m_aRetryCallback,
                                                                                   m_aResponseConsumer,
                                                                                   m_aSignalMsgConsumer);
      }
      catch (final Phase4Exception ex)
      {
        // Re-throw
        throw ex;
      }
      catch (final Exception ex)
      {
        // wrap
        throw new Phase4Exception ("Wrapped Phase4Exception", ex);
      }
    }
  }

  /**
   * The builder class for sending AS4 messages using BDEW profile specifics.
   * Use {@link #sendMessage()} to trigger the main transmission.
   *
   * @author Philip Helger
   */
  public static class BDEWUserMessageBuilder extends AbstractBDEWUserMessageBuilder<BDEWUserMessageBuilder>
  {
    public BDEWUserMessageBuilder()
    {}
  }

  /**
   * Additional parameters to add in the PayloadInfo part of AS4 UserMessage
   *
   * @author Gregor Scholtysik
   */
  @NotThreadSafe
  public static class BDEWPayloadParams
  {
    private String m_sDocumentType;
    private ZonedDateTime m_sDocumentDate;
    private Integer m_sDocumentNumber;
    private ZonedDateTime m_sFulfillmentDate;
    private String m_sSubjectPartyID;
    private String m_sSubjectPartyRole;

    /**
     * @return BDEW payload document type for payload identifier <code>BDEWDocumentType</code>
     */
    @Nullable
    public String getDocumentType ()
    {
      return m_sDocumentType;
    }

    /**
     * BDEW payload document type
     *
     * @param sDocumentType
     *        Document type to use. May be <code>null</code>.
     */
    public void setDocumentType (@Nullable final String sDocumentType)
    {
      m_sDocumentType = sDocumentType;
    }

    /**
     * @return BDEW payload document date for payload identifier <code>BDEWDocumentDate</code>
     */
    @Nullable
    public ZonedDateTime getDocumentDate ()
    {
      return m_sDocumentDate;
    }

    /**
     * BDEW payload document date
     *
     * @param sDocumentDate
     *        Document date to use. May be <code>null</code>.
     */
    public void setDocumentDate (@Nullable final ZonedDateTime sDocumentDate)
    {
      m_sDocumentDate = sDocumentDate;
    }

    /**
     * @return BDEW payload document number for payload identifier <code>BDEWDocumentNo</code>
     */
    @Nullable
    public Integer getDocumentNumber ()
    {
      return m_sDocumentNumber;
    }

    /**
     * BDEW payload document number
     *
     * @param sDocumentNumber
     *        Document number to use. May be <code>null</code>.
     */
    public void setDocumentNumber (@Nullable final Integer sDocumentNumber)
    {
      m_sDocumentNumber = sDocumentNumber;
    }


    /**
     * @return BDEW payload fulfillment date for payload identifier <code>BDEWFulfillmentDate</code>
     */
    @Nullable
    public ZonedDateTime getFulfillmentDate ()
    {
      return m_sFulfillmentDate;
    }

    /**
     * BDEW payload fulfillment date
     *
     * @param sFulfillmenttDate
     *        Fulfillment date to use. May be <code>null</code>.
     */
    public void setFulfillmentDate (@Nullable final ZonedDateTime sFulfillmenttDate)
    {
      m_sFulfillmentDate = sFulfillmenttDate;
    }

    /**
     * @return BDEW payload subject party ID for payload identifier <code>BDEWSubjectPartyID</code>
     */
    @Nullable
    public String getSubjectPartyId ()
    {
      return m_sSubjectPartyID;
    }

    /**
     * BDEW payload subject party ID
     *
     * @param sSubjectPartyId
     *        Subject party ID to use. May be <code>null</code>.
     */
    public void setSubjectPartyId (@Nullable final String sSubjectPartyId)
    {
      m_sSubjectPartyID = sSubjectPartyId;
    }

    /**
     * @return BDEW payload subject party ID for payload identifier <code>BDEWSubjectPartyRole</code>
     */
    @Nullable
    public String getSubjectPartyRole ()
    {
      return m_sSubjectPartyRole;
    }

    /**
     * BDEW payload subject party role
     *
     * @param sSubjectPartyRole
     *        Subject party role to use. May be <code>null</code>.
     */
    public void setSubjectPartyRole (@Nullable final String sSubjectPartyRole)
    {
      m_sSubjectPartyRole = sSubjectPartyRole;
    }
  }
}