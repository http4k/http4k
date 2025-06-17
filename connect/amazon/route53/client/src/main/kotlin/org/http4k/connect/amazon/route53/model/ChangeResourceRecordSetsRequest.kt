package org.http4k.connect.amazon.route53.model

data class Change(
    val action: String,
    val resourceRecordSet: List<ResourceRecordSet>
)

data class ResourceRecordSet(
    val aliasTarget: AliasTarget,
    val ttl: Long,
    val type: String
)

data class AliasTarget(
    val dnsName: String,
    val hostedZoneId: String,
    val evaluateTargetHealth: Boolean,
    val resourceRecords: List<ResourceRecord>
)

data class ResourceRecord(
    val value: String
)

/*
<ChangeResourceRecordSetsRequest xmlns="https://route53.amazonaws.com/doc/2013-04-01/">
   <ChangeBatch>
      <Changes>
         <Change>
            <Action>string</Action>
            <ResourceRecordSet>
               <AliasTarget>
                  <DNSName>string</DNSName>
                  <EvaluateTargetHealth>boolean</EvaluateTargetHealth>
                  <HostedZoneId>string</HostedZoneId>
               </AliasTarget>
               <CidrRoutingConfig>
                  <CollectionId>string</CollectionId>
                  <LocationName>string</LocationName>
               </CidrRoutingConfig>
               <Failover>string</Failover>
               <GeoLocation>
                  <ContinentCode>string</ContinentCode>
                  <CountryCode>string</CountryCode>
                  <SubdivisionCode>string</SubdivisionCode>
               </GeoLocation>
               <GeoProximityLocation>
                  <AWSRegion>string</AWSRegion>
                  <Bias>integer</Bias>
                  <Coordinates>
                     <Latitude>string</Latitude>
                     <Longitude>string</Longitude>
                  </Coordinates>
                  <LocalZoneGroup>string</LocalZoneGroup>
               </GeoProximityLocation>
               <HealthCheckId>string</HealthCheckId>
               <MultiValueAnswer>boolean</MultiValueAnswer>
               <Name>string</Name>
               <Region>string</Region>
               <ResourceRecords>
                  <ResourceRecord>
                     <Value>string</Value>
                  </ResourceRecord>
               </ResourceRecords>
               <SetIdentifier>string</SetIdentifier>
               <TrafficPolicyInstanceId>string</TrafficPolicyInstanceId>
               <TTL>long</TTL>
               <Type>string</Type>
               <Weight>long</Weight>
            </ResourceRecordSet>
         </Change>
      </Changes>
      <Comment>string</Comment>
   </ChangeBatch>
</ChangeResourceRecordSetsRequest>
 */
