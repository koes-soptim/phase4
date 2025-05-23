/*
 * Copyright (C) 2015-2025 Philip Helger (www.helger.com)
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
package com.helger.phase4.test.profile;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.state.ETriState;
import com.helger.phase4.CAS4;
import com.helger.phase4.crypto.ECryptoAlgorithmCrypt;
import com.helger.phase4.crypto.ECryptoAlgorithmSign;
import com.helger.phase4.crypto.ECryptoAlgorithmSignDigest;
import com.helger.phase4.mgr.MetaAS4Manager;
import com.helger.phase4.model.EMEP;
import com.helger.phase4.model.EMEPBinding;
import com.helger.phase4.model.pmode.IPModeIDProvider;
import com.helger.phase4.model.pmode.PMode;
import com.helger.phase4.model.pmode.PModeParty;
import com.helger.phase4.model.pmode.PModePayloadService;
import com.helger.phase4.model.pmode.PModeReceptionAwareness;
import com.helger.phase4.model.pmode.leg.EPModeSendReceiptReplyPattern;
import com.helger.phase4.model.pmode.leg.PModeAddressList;
import com.helger.phase4.model.pmode.leg.PModeLeg;
import com.helger.phase4.model.pmode.leg.PModeLegBusinessInformation;
import com.helger.phase4.model.pmode.leg.PModeLegErrorHandling;
import com.helger.phase4.model.pmode.leg.PModeLegProtocol;
import com.helger.phase4.model.pmode.leg.PModeLegReliability;
import com.helger.phase4.model.pmode.leg.PModeLegSecurity;
import com.helger.phase4.wss.EWSSVersion;

/**
 * Test PMode creation code.
 *
 * @author Philip Helger
 * @since 2.3.0
 */
@Immutable
public final class TestPMode
{
  public static final String DEFAULT_AGREEMENT_ID = "urn:as4:agreement";

  private TestPMode ()
  {}

  @Nonnull
  public static PModeLegProtocol generatePModeLegProtocol (@Nullable final String sAddress)
  {
    // Set the endpoint URL
    return PModeLegProtocol.createForDefaultSoapVersion (sAddress);
  }

  @Nonnull
  public static PModeLegBusinessInformation generatePModeLegBusinessInformation ()
  {
    final String sService = null;
    final String sAction = CAS4.DEFAULT_ACTION_URL;
    final Long nPayloadProfileMaxKB = null;
    final String sMPCID = CAS4.DEFAULT_MPC_ID;
    return PModeLegBusinessInformation.create (sService, sAction, nPayloadProfileMaxKB, sMPCID);
  }

  @Nonnull
  public static PModeLegErrorHandling generatePModeLegErrorHandling ()
  {
    final PModeAddressList aReportSenderErrorsTo = null;
    final PModeAddressList aReportReceiverErrorsTo = null;
    final ETriState eReportAsResponse = ETriState.TRUE;
    final ETriState eReportProcessErrorNotifyConsumer = ETriState.TRUE;
    final ETriState eReportProcessErrorNotifyProducer = ETriState.TRUE;
    final ETriState eReportDeliveryFailuresNotifyProducer = ETriState.TRUE;
    return new PModeLegErrorHandling (aReportSenderErrorsTo,
                                      aReportReceiverErrorsTo,
                                      eReportAsResponse,
                                      eReportProcessErrorNotifyConsumer,
                                      eReportProcessErrorNotifyProducer,
                                      eReportDeliveryFailuresNotifyProducer);
  }

  @Nonnull
  public static PModeLegSecurity generatePModeLegSecurity ()
  {
    final PModeLegSecurity aPModeLegSecurity = new PModeLegSecurity ();
    aPModeLegSecurity.setWSSVersion (EWSSVersion.WSS_111);
    aPModeLegSecurity.setX509SignatureAlgorithm (ECryptoAlgorithmSign.RSA_SHA_256);
    aPModeLegSecurity.setX509SignatureHashFunction (ECryptoAlgorithmSignDigest.DIGEST_SHA_256);
    aPModeLegSecurity.setX509EncryptionAlgorithm (ECryptoAlgorithmCrypt.AES_128_GCM);
    aPModeLegSecurity.setX509EncryptionMinimumStrength (128);
    aPModeLegSecurity.setPModeAuthorize (false);
    aPModeLegSecurity.setSendReceipt (true);
    aPModeLegSecurity.setSendReceiptNonRepudiation (true);
    aPModeLegSecurity.setSendReceiptReplyPattern (EPModeSendReceiptReplyPattern.RESPONSE);
    return aPModeLegSecurity;
  }

  @Nonnull
  public static PModeLeg generatePModeLeg (@Nullable final String sResponderAddress)
  {
    return new PModeLeg (generatePModeLegProtocol (sResponderAddress),
                         generatePModeLegBusinessInformation (),
                         generatePModeLegErrorHandling (),
                         (PModeLegReliability) null,
                         generatePModeLegSecurity ());
  }

  @Nonnull
  public static PModeReceptionAwareness generatePModeReceptionAwareness ()
  {
    final ETriState eReceptionAwareness = ETriState.TRUE;
    final ETriState eRetry = ETriState.TRUE;
    final int nMaxRetries = 1;
    final long nRetryIntervalMS = 10_000;
    final ETriState eDuplicateDetection = ETriState.TRUE;
    return new PModeReceptionAwareness (eReceptionAwareness,
                                        eRetry,
                                        nMaxRetries,
                                        nRetryIntervalMS,
                                        eDuplicateDetection);
  }

  /**
   * One-Way Version of the Test pmode uses one-way push
   *
   * @param sInitiatorID
   *        Initiator ID
   * @param sResponderID
   *        Responder ID
   * @param sResponderAddress
   *        Responder URL
   * @param aPModeIDProvider
   *        PMode ID provider
   * @param bPersist
   *        <code>true</code> to persist the PMode in the PModeManager,
   *        <code>false</code> to have it only in memory.
   * @return New PMode
   */
  @Nonnull
  public static PMode createTestPMode (@Nonnull @Nonempty final String sInitiatorID,
                                       @Nonnull @Nonempty final String sResponderID,
                                       @Nonnull @Nonempty final String sResponderAddress,
                                       @Nonnull final IPModeIDProvider aPModeIDProvider,
                                       final boolean bPersist)
  {
    ValueEnforcer.notEmpty (sInitiatorID, "InitiatorID");
    ValueEnforcer.notEmpty (sResponderID, "ResponderID");
    ValueEnforcer.notEmpty (sResponderAddress, "ResponderAddress");

    final PModeParty aInitiator = PModeParty.createSimple (sInitiatorID, CAS4.DEFAULT_INITIATOR_URL);
    final PModeParty aResponder = PModeParty.createSimple (sResponderID, CAS4.DEFAULT_RESPONDER_URL);

    final PMode aPMode = new PMode (aPModeIDProvider.getPModeID (aInitiator, aResponder),
                                    aInitiator,
                                    aResponder,
                                    DEFAULT_AGREEMENT_ID,
                                    EMEP.ONE_WAY,
                                    EMEPBinding.PUSH,
                                    generatePModeLeg (sResponderAddress),
                                    (PModeLeg) null,
                                    (PModePayloadService) null,
                                    generatePModeReceptionAwareness ());
    // Leg 2 stays null, because we only use one-way
    // By default no compression active

    if (bPersist)
    {
      // Ensure it is stored
      MetaAS4Manager.getPModeMgr ().createOrUpdatePMode (aPMode);
    }
    return aPMode;
  }

  /**
   * Two-Way Version of the Test pmode uses two-way push-push
   *
   * @param sInitiatorID
   *        Initiator ID
   * @param sResponderID
   *        Responder ID
   * @param sResponderAddress
   *        Responder URL
   * @param aPModeIDProvider
   *        PMode ID provider
   * @param bPersist
   *        <code>true</code> to persist the PMode <code>false</code> to have it
   *        only in memory.
   * @return New PMode
   */
  @Nonnull
  public static PMode createTestPModeTwoWay (@Nonnull @Nonempty final String sInitiatorID,
                                             @Nonnull @Nonempty final String sResponderID,
                                             @Nullable final String sResponderAddress,
                                             @Nonnull final IPModeIDProvider aPModeIDProvider,
                                             final boolean bPersist)
  {
    final PModeParty aInitiator = PModeParty.createSimple (sInitiatorID, CAS4.DEFAULT_INITIATOR_URL);
    final PModeParty aResponder = PModeParty.createSimple (sResponderID, CAS4.DEFAULT_RESPONDER_URL);

    final PMode aPMode = new PMode (aPModeIDProvider.getPModeID (aInitiator, aResponder),
                                    aInitiator,
                                    aResponder,
                                    DEFAULT_AGREEMENT_ID,
                                    EMEP.TWO_WAY,
                                    EMEPBinding.PUSH_PUSH,
                                    generatePModeLeg (sResponderAddress),
                                    generatePModeLeg (sResponderAddress),
                                    (PModePayloadService) null,
                                    PModeReceptionAwareness.createDefault ());
    if (bPersist)
    {
      // Ensure it is stored
      MetaAS4Manager.getPModeMgr ().createOrUpdatePMode (aPMode);
    }
    return aPMode;
  }
}
