package org.openmrs.module.bahmniemrapi.encountertransaction.impl;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.openmrs.Encounter;
import org.openmrs.Location;
import org.openmrs.Patient;
import org.openmrs.Visit;
import org.openmrs.api.EncounterService;
import org.openmrs.api.PatientService;
import org.openmrs.api.VisitService;
import org.openmrs.module.bahmniemrapi.encountertransaction.contract.BahmniEncounterSearchParameters;
import org.openmrs.module.bahmniemrapi.encountertransaction.mapper.BahmniEncounterTransactionMapper;
import org.openmrs.module.bahmniemrapi.encountertransaction.service.BahmniEncounterTransactionService;
import org.openmrs.module.bahmniemrapi.visitlocation.BahmniVisitLocationService;
import org.openmrs.module.emrapi.encounter.EncounterParameters;
import org.openmrs.module.emrapi.encounter.EncounterTransactionMapper;
import org.openmrs.module.emrapi.encounter.domain.EncounterTransaction;
import org.openmrs.module.emrapi.encounter.matcher.BaseEncounterMatcher;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class BahmniEncounterTransactionServiceImplTest {


    @Mock
    private BaseEncounterMatcher baseEncounterMatcher;

    @Mock
    private VisitService visitService;

    @Mock
    private EncounterService encounterService;

    @Mock
    private PatientService patientService;

    @Mock
    private EncounterTransactionMapper encounterTransactionMapper;

    @Mock
    private BahmniVisitLocationService bahmniVisitLocationService;


    private BahmniEncounterTransactionService bahmniEncounterTransactionService;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        bahmniEncounterTransactionService = new BahmniEncounterTransactionServiceImpl(encounterService,null,encounterTransactionMapper,null,null,null,null,visitService,patientService
                ,null,null,baseEncounterMatcher,bahmniVisitLocationService);

    }

    @Test
    public void testFind() throws Exception {

    }

    @Test
    public void shouldNotReturnTheEncounterFromTheVisitThatIsOpenedInOtherVisitLocation() throws Exception {
        Location location = new Location();
        location.setUuid("visit-location-uuid");
        Visit visit = new Visit();
        visit.setLocation(location);
        visit.setUuid("visit-uuid");

        BahmniEncounterSearchParameters encounterSearchParameters = new BahmniEncounterSearchParameters();
        encounterSearchParameters.setLocationUuid("login-location-uuid");
        encounterSearchParameters.setPatientUuid("patient-uuid");
        encounterSearchParameters.setEncounterTypeUuids(Arrays.asList("encounter-type-uuid"));
        when(baseEncounterMatcher.findEncounter(any(Visit.class), any(EncounterParameters.class))).thenReturn(null);
        when(visitService.getActiveVisitsByPatient(any(Patient.class))).thenReturn(Arrays.asList(visit));
        when(encounterService.getEncounterByUuid(anyString())).thenReturn(null);
        when(bahmniVisitLocationService.getVisitLocationForLoginLocation(anyString())).thenReturn("visit-location");

        bahmniEncounterTransactionService.find(encounterSearchParameters);
        ArgumentCaptor<Visit> argumentCaptor = ArgumentCaptor.forClass(Visit.class);
        ArgumentCaptor<EncounterParameters> argument = ArgumentCaptor.forClass(EncounterParameters.class);
        verify(baseEncounterMatcher).findEncounter(argumentCaptor.capture(), argument.capture());
        assertEquals(argumentCaptor.getValue(), null);
    }

    @Test
    public void shouldReturnTheEncounterFromTheVisitThatIsOpenedInThatVisitLocation() throws Exception {
        EncounterTransaction encounterTransaction = new EncounterTransaction();
        encounterTransaction.setEncounterUuid("encounter-uuid");

        Location location = new Location();
        location.setUuid("visit-location-uuid");
        Visit visit = new Visit();
        visit.setLocation(location);
        visit.setUuid("visit-uuid");

        Encounter encounter = new Encounter();
        encounter.setLocation(location);
        encounter.setUuid("encounter-uuid");
        HashSet<Encounter> encounters = new HashSet<>();
        encounters.add(encounter);
        visit.setEncounters(encounters);

        BahmniEncounterSearchParameters encounterSearchParameters = new BahmniEncounterSearchParameters();
        encounterSearchParameters.setLocationUuid("login-location-uuid");
        encounterSearchParameters.setPatientUuid("patient-uuid");
        encounterSearchParameters.setEncounterTypeUuids(Arrays.asList("encounter-type-uuid"));
        when(baseEncounterMatcher.findEncounter(any(Visit.class), any(EncounterParameters.class))).thenReturn(encounter);
        List<Visit> visits = Arrays.asList(visit);
        when(visitService.getActiveVisitsByPatient(any(Patient.class))).thenReturn(visits);
        when(encounterService.getEncounterByUuid(anyString())).thenReturn(null);
        when(bahmniVisitLocationService.getVisitLocationForLoginLocation(anyString())).thenReturn("visit-location-uuid");
        when(bahmniVisitLocationService.getMatchingVisitInLocation(visits, "login-location-uuid")).thenReturn(visit);
        when(encounterTransactionMapper.map(any(Encounter.class),anyBoolean())).thenReturn(encounterTransaction);
        bahmniEncounterTransactionService.find(encounterSearchParameters);
        ArgumentCaptor<Visit> argumentCaptor = ArgumentCaptor.forClass(Visit.class);
        ArgumentCaptor<EncounterParameters> argument = ArgumentCaptor.forClass(EncounterParameters.class);
        verify(baseEncounterMatcher).findEncounter(argumentCaptor.capture(), argument.capture());
        assertEquals(argumentCaptor.getValue().getUuid(), "visit-uuid");
    }

    @Test
    public void shouldReturnTheEncounterFromTheVisitThatIsOpenedInThatVisitLocationIfThereAreTwoVisitsInDiffLocations() throws Exception {
        EncounterTransaction encounterTransaction = new EncounterTransaction();
        encounterTransaction.setEncounterUuid("encounter-uuid");

        Location location = new Location();
        location.setUuid("visit-location-uuid");

        Visit visitOne = new Visit();
        visitOne.setLocation(location);
        visitOne.setUuid("visit-uuid-one");

        Location locationTwo = new Location();
        locationTwo.setUuid("visit-location-uuid-two");

        Visit visitTwo = new Visit();
        visitTwo.setUuid("visit-uuid-two");
        visitTwo.setLocation(locationTwo);

        Encounter encounter = new Encounter();
        encounter.setLocation(location);
        encounter.setUuid("encounter-uuid");
        HashSet<Encounter> encounters = new HashSet<>();
        encounters.add(encounter);
        visitTwo.setEncounters(encounters);

        BahmniEncounterSearchParameters encounterSearchParameters = new BahmniEncounterSearchParameters();
        encounterSearchParameters.setLocationUuid("login-location-uuid");
        encounterSearchParameters.setPatientUuid("patient-uuid");
        encounterSearchParameters.setEncounterTypeUuids(Arrays.asList("encounter-type-uuid"));
        when(baseEncounterMatcher.findEncounter(any(Visit.class), any(EncounterParameters.class))).thenReturn(encounter);
        List<Visit> visits = Arrays.asList(visitOne, visitTwo);
        when(visitService.getActiveVisitsByPatient(any(Patient.class))).thenReturn(visits);
        when(encounterService.getEncounterByUuid(anyString())).thenReturn(null);
        when(bahmniVisitLocationService.getVisitLocationForLoginLocation(anyString())).thenReturn("visit-location-uuid-two");
        when(bahmniVisitLocationService.getMatchingVisitInLocation(visits, "login-location-uuid")).thenReturn(visitTwo);

        bahmniEncounterTransactionService.find(encounterSearchParameters);
        ArgumentCaptor<Visit> argumentCaptor = ArgumentCaptor.forClass(Visit.class);
        ArgumentCaptor<EncounterParameters> argument = ArgumentCaptor.forClass(EncounterParameters.class);
        verify(baseEncounterMatcher).findEncounter(argumentCaptor.capture(), argument.capture());
        assertEquals(argumentCaptor.getValue().getUuid(), "visit-uuid-two");
    }

    @Test
    public void shouldReturnTheEncounterFromTheVisitWithoutLocationIfThereAreTwoActiveVisitsOneWithLocationNullAndOneWithDiffVisitLocationSet() throws Exception {
        Location location = new Location();
        location.setUuid("visit-location-uuid-one");

        Visit visitOne = new Visit();
        visitOne.setLocation(location);
        visitOne.setUuid("visit-uuid-one");

        Visit visitTwo = new Visit();
        visitTwo.setUuid("visit-uuid-two");
        visitTwo.setLocation(null);

        Encounter encounter = new Encounter();
        encounter.setLocation(location);
        encounter.setUuid("encounter-uuid");
        HashSet<Encounter> encounters = new HashSet<>();
        encounters.add(encounter);
        visitTwo.setEncounters(encounters);

        BahmniEncounterSearchParameters encounterSearchParameters = new BahmniEncounterSearchParameters();
        encounterSearchParameters.setLocationUuid("login-location-uuid");
        encounterSearchParameters.setPatientUuid("patient-uuid");
        encounterSearchParameters.setEncounterTypeUuids(Arrays.asList("encounter-type-uuid"));
        when(baseEncounterMatcher.findEncounter(any(Visit.class), any(EncounterParameters.class))).thenReturn(encounter);
        List<Visit> visits = Arrays.asList(visitOne, visitTwo);
        when(visitService.getActiveVisitsByPatient(any(Patient.class))).thenReturn(visits);
        when(bahmniVisitLocationService.getVisitLocationForLoginLocation(anyString())).thenReturn("visit-location-uuid-two");
        when(encounterService.getEncounterByUuid(anyString())).thenReturn(null);
        when(bahmniVisitLocationService.getMatchingVisitInLocation(visits, "login-location-uuid")).thenReturn(visitTwo);

        bahmniEncounterTransactionService.find(encounterSearchParameters);
        ArgumentCaptor<Visit> argumentCaptor = ArgumentCaptor.forClass(Visit.class);
        ArgumentCaptor<EncounterParameters> argument = ArgumentCaptor.forClass(EncounterParameters.class);
        verify(baseEncounterMatcher).findEncounter(argumentCaptor.capture(), argument.capture());
        assertEquals(argumentCaptor.getValue().getUuid(), "visit-uuid-two");
    }


//    @Test
//    public void shouldReturnTheEncounterFromTheVisitWithoutLocationIfThereAreTwoActiveVisitsOneWithLocationNullAndOneWithDiffVisitLocationSet() throws Exception {
//        Location location = new Location();
//        location.setUuid("visit-location-uuid-one");
//
//        Visit visitOne = new Visit();
//        visitOne.setLocation(location);
//        visitOne.setUuid("visit-uuid-one");
//
//        Visit visitTwo = new Visit();
//        visitTwo.setUuid("visit-uuid-two");
//        visitTwo.setLocation(null);
//
//
//        BahmniEncounterSearchParameters encounterSearchParameters = new BahmniEncounterSearchParameters();
//        encounterSearchParameters.setLocationUuid("login-location-uuid");
//        encounterSearchParameters.setPatientUuid("patient-uuid");
//        encounterSearchParameters.setEncounterTypeUuids(Arrays.asList("encounter-type-uuid"));
//        when(baseEncounterMatcher.findEncounter(any(Visit.class), any(EncounterParameters.class))).thenReturn(null);
//        when(visitService.getActiveVisitsByPatient(any(Patient.class))).thenReturn(Arrays.asList(visitOne, visitTwo));
//        when(encounterService.getEncounterByUuid(anyString())).thenReturn(null);
//        when(bahmniVisitLocationService.getVisitLocationForLoginLocation(anyString())).thenReturn("visit-location-uuid");
//
//        bahmniEncounterTransactionService.find(encounterSearchParameters);
//        ArgumentCaptor<Visit> argumentCaptor = ArgumentCaptor.forClass(Visit.class);
//        ArgumentCaptor<EncounterParameters> argument = ArgumentCaptor.forClass(EncounterParameters.class);
//        verify(baseEncounterMatcher).findEncounter(argumentCaptor.capture(), argument.capture());
//        assertEquals(argumentCaptor.getValue().getUuid(), "visit-uuid-two");
//    }

    @Test
    public void shouldReturnTheEncounterFromTheVisitThatIsOpenedInThatVisitLocationIfThereAreTwoVisitsOneWithLocationNullAndOneWithVisitLocationSet() throws Exception {
        EncounterTransaction encounterTransaction = new EncounterTransaction();
        encounterTransaction.setEncounterUuid("encounter-uuid");

        Location location = new Location();
        location.setUuid("visit-location-uuid");

        Visit visitOne = new Visit();
        visitOne.setLocation(location);
        visitOne.setUuid("visit-uuid-one");

        Visit visitTwo = new Visit();
        visitTwo.setUuid("visit-uuid-two");
        visitTwo.setLocation(null);

        Encounter encounter = new Encounter();
        encounter.setLocation(location);
        encounter.setUuid("encounter-uuid");
        HashSet<Encounter> encounters = new HashSet<>();
        encounters.add(encounter);
        visitOne.setEncounters(encounters);

        BahmniEncounterSearchParameters encounterSearchParameters = new BahmniEncounterSearchParameters();
        encounterSearchParameters.setLocationUuid("login-location-uuid");
        encounterSearchParameters.setPatientUuid("patient-uuid");
        encounterSearchParameters.setEncounterTypeUuids(Arrays.asList("encounter-type-uuid"));
        when(baseEncounterMatcher.findEncounter(any(Visit.class), any(EncounterParameters.class))).thenReturn(encounter);
        List<Visit> visits = Arrays.asList(visitOne, visitTwo);
        when(visitService.getActiveVisitsByPatient(any(Patient.class))).thenReturn(visits);
        when(encounterService.getEncounterByUuid(anyString())).thenReturn(null);
        when(bahmniVisitLocationService.getVisitLocationForLoginLocation(anyString())).thenReturn("visit-location-uuid");
        when(bahmniVisitLocationService.getMatchingVisitInLocation(visits, "login-location-uuid")).thenReturn(visitOne);
        when(encounterTransactionMapper.map(any(Encounter.class),anyBoolean())).thenReturn(encounterTransaction);

        bahmniEncounterTransactionService.find(encounterSearchParameters);
        ArgumentCaptor<Visit> argumentCaptor = ArgumentCaptor.forClass(Visit.class);
        ArgumentCaptor<EncounterParameters> argument = ArgumentCaptor.forClass(EncounterParameters.class);
        verify(baseEncounterMatcher).findEncounter(argumentCaptor.capture(), argument.capture());
        assertEquals(argumentCaptor.getValue().getUuid(), "visit-uuid-one");
    }

    @Test
    public void shouldReturnEncounterCreatedInThatVisitLocationInRetrospectiveMode() throws ParseException {
        EncounterTransaction encounterTransaction = new EncounterTransaction();
        encounterTransaction.setEncounterUuid("encounter-uuid");
        Location location = new Location();
        location.setUuid("login-location-uuid");

        Visit visitOne = new Visit();
        Encounter encounter = new Encounter();
        encounter.setLocation(location);
        encounter.setUuid("encounter-uuid");

        BahmniEncounterSearchParameters encounterSearchParameters = new BahmniEncounterSearchParameters();
        encounterSearchParameters.setLocationUuid("login-location-uuid");
        encounterSearchParameters.setPatientUuid("patient-uuid");
        encounterSearchParameters.setEncounterTypeUuids(Arrays.asList("encounter-type-uuid"));
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd-MM-yyyy");
        Date encounterDateTimeTo = simpleDateFormat.parse("20-10-2015");
        Date encounterDateTimeFrom = simpleDateFormat.parse("10-10-2015");
        encounterSearchParameters.setEncounterDateTimeTo(encounterDateTimeTo);
        encounterSearchParameters.setEncounterDateTimeFrom(encounterDateTimeFrom);

        when(baseEncounterMatcher.findEncounter(any(Visit.class), any(EncounterParameters.class))).thenReturn(encounter);
        when(visitService.getActiveVisitsByPatient(any(Patient.class))).thenReturn(Arrays.asList(visitOne));
        when(encounterService.getEncounterByUuid(anyString())).thenReturn(null);
        when(encounterTransactionMapper.map(any(Encounter.class),anyBoolean())).thenReturn(encounterTransaction);
        when(bahmniVisitLocationService.getVisitLocationForLoginLocation("login-location-uuid")).thenReturn("visit-location-uuid");

        EncounterTransaction savedEncounterTransaction = bahmniEncounterTransactionService.find(encounterSearchParameters);

        assertEquals(savedEncounterTransaction.getEncounterUuid(), "encounter-uuid");
    }

    @Test
    public void shouldNotReturnEncounterIfItIsNotCreatedInThatVisitLocationInRetrospectiveMode() throws ParseException {
        EncounterTransaction encounterTransaction = new EncounterTransaction();
        encounterTransaction.setEncounterUuid("encounter-uuid");
        Location location = new Location();
        location.setUuid("encounter-location-uuid");

        Visit visitOne = new Visit();
        Encounter encounter = new Encounter();
        encounter.setLocation(location);
        encounter.setUuid("encounter-uuid");

        BahmniEncounterSearchParameters encounterSearchParameters = new BahmniEncounterSearchParameters();
        encounterSearchParameters.setLocationUuid("login-location-uuid");
        encounterSearchParameters.setPatientUuid("patient-uuid");
        encounterSearchParameters.setEncounterTypeUuids(Arrays.asList("encounter-type-uuid"));
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd-MM-yyyy");
        Date encounterDateTimeTo = simpleDateFormat.parse("20-10-2015");
        Date encounterDateTimeFrom = simpleDateFormat.parse("10-10-2015");
        encounterSearchParameters.setEncounterDateTimeTo(encounterDateTimeTo);
        encounterSearchParameters.setEncounterDateTimeFrom(encounterDateTimeFrom);

        when(baseEncounterMatcher.findEncounter(any(Visit.class), any(EncounterParameters.class))).thenReturn(encounter);
        when(visitService.getActiveVisitsByPatient(any(Patient.class))).thenReturn(Arrays.asList(visitOne));
        when(encounterService.getEncounterByUuid(anyString())).thenReturn(null);
        when(encounterTransactionMapper.map(any(Encounter.class),anyBoolean())).thenReturn(encounterTransaction);
        when(bahmniVisitLocationService.getVisitLocationForLoginLocation("encounter-location-uuid")).thenReturn("visit-location-uuid-one");
        when(bahmniVisitLocationService.getVisitLocationForLoginLocation("login-location-uuid")).thenReturn("visit-location-uuid");

        EncounterTransaction savedEncounterTransaction = bahmniEncounterTransactionService.find(encounterSearchParameters);

        assertNull(savedEncounterTransaction);
    }
}