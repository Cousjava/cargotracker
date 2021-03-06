package org.eclipse.cargotracker.interfaces.handling.mobile;

import org.eclipse.cargotracker.application.ApplicationEvents;
import org.eclipse.cargotracker.application.util.LocationUtil;
import org.eclipse.cargotracker.domain.model.cargo.TrackingId;
import org.eclipse.cargotracker.domain.model.handling.HandlingEvent;
import org.eclipse.cargotracker.domain.model.location.UnLocode;
import org.eclipse.cargotracker.domain.model.voyage.Voyage;
import org.eclipse.cargotracker.domain.model.voyage.VoyageNumber;
import org.eclipse.cargotracker.domain.model.voyage.VoyageRepository;
import org.eclipse.cargotracker.interfaces.booking.facade.BookingServiceFacade;
import org.eclipse.cargotracker.interfaces.booking.facade.dto.CargoRoute;
import org.eclipse.cargotracker.interfaces.handling.HandlingEventRegistrationAttempt;
import org.primefaces.event.FlowEvent;

import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.model.SelectItem;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Named
@ViewScoped
public class EventWizard implements Serializable {

    @Inject
    private BookingServiceFacade bookingServiceFacade;

    @Inject
    private ApplicationEvents applicationEvents;

    @Inject
    private VoyageRepository voyageRepository;


    private List<SelectItem> trackIds;
    private List<SelectItem> locations;
    private List<SelectItem> voyages;

    /**
     * wizard's data
     */
    private String voyageNumber;
    private Date completionDate;
    private String eventType;
    private String location;
    private String trackId;

    @PostConstruct
    public void init() {
        List<CargoRoute> cargos = bookingServiceFacade.listAllCargos();

        // fill the TrackingId dropdown list
        trackIds = new ArrayList<>();
        for (CargoRoute route : cargos) {
            if (route.isRouted() && !route.isClaimed()) { // we just need getRoutedUnclaimedCargos
                String routedUnclaimedId = route.getTrackingId();
                trackIds.add(new SelectItem(routedUnclaimedId, routedUnclaimedId));
            }
        }

        // fill the Port dropdown list
        locations = new ArrayList<>();
        List<String> allLocations = LocationUtil.getLocationsCode();
        for (String tempLoc : allLocations) {
            locations.add(new SelectItem(tempLoc, tempLoc));
        }

        // fill the Voyage dropdown list (only needed for LOAD & UNLOAD events)
        List<Voyage> allVoyages = voyageRepository.findAll();
        List<SelectItem> allVoyagesModel = new ArrayList<>(allVoyages.size());

        for (Voyage voyage : allVoyages) {
            allVoyagesModel.add(new SelectItem(voyage.getVoyageNumber().getIdString(),
                    voyage.getVoyageNumber().getIdString()));
        }

        this.voyages = allVoyagesModel;
    }

    public String onFlowProcess(FlowEvent event) {
        // here we can customize the flow of the wizard

        // Voyage is set only for LOAD and UNLOAD events.
        // For other events, it is null.
        // Thus we can skip in those cases the voyage tab and go straight to the date tab.
        if (event.getOldStep().equals("eventTab")
                && !"LOAD".equals(eventType) && !"UNLOAD".equals(eventType))
            return "dateTab";
        return event.getNewStep(); // default flow of the wizard
    }


    public void save() {
        VoyageNumber selectedVoyage = null;

        Date registrationTime = new Date();
        TrackingId trackingId = new TrackingId(trackId);
        UnLocode unLocode = new UnLocode(this.location);
        HandlingEvent.Type type = HandlingEvent.Type.valueOf(eventType);

        if (voyageNumber != null) {  // Only Load & Unload could have a Voyage set
            selectedVoyage = new VoyageNumber(voyageNumber);
        }

        HandlingEventRegistrationAttempt attempt
                = new HandlingEventRegistrationAttempt(registrationTime, completionDate, trackingId, selectedVoyage, type, unLocode);

        applicationEvents.receivedHandlingEventRegistrationAttempt(attempt);

        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage("Event submitted", ""));
    }

    public void setTrackId(String trackId) {
        this.trackId = trackId;
    }

    public void setVoyageNumber(String voyageNumber) {
        this.voyageNumber = voyageNumber;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public void setCompletionDate(Date completionDate) {
        this.completionDate = completionDate;
    }

    public String getTrackId() {
        return trackId;
    }

    public List<SelectItem> getTrackIds() {
        return trackIds;
    }

    public String getVoyageNumber() {
        return voyageNumber;
    }

    public List<SelectItem> getVoyages() {
        return voyages;
    }

    public String getLocation() {
        return location;
    }

    public List<SelectItem> getLocations() {
        return locations;
    }

    public String getEventType() {
        return eventType;
    }

    public Date getCompletionDate() {
        return completionDate;
    }
}
