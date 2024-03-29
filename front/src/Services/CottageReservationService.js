import axios from "axios";
import LogInService from "@/Services/LogInService";

const URL = process.env.VUE_APP_BACKEND_URL + 'cottageReservation';

class CottageReservationService {

    getAllReservationsOfOwnerForCalendar(ownerId){
        const headers = {
            'Accept': 'application/json',
            'Content-Type': 'application/json',
            'Authorization': 'Bearer ' + LogInService.accessToken
        };
        return axios.get(URL + '/calendar/' + ownerId, {headers});
    }

    getAllReservationsOfCottageForCalendar(cottageId){
        const headers = {
            'Accept': 'application/json',
            'Content-Type': 'application/json',
            'Authorization': 'Bearer ' + LogInService.accessToken
        };
        return axios.get(URL + '/calendarCottage/' + cottageId, {headers});
    }

    getAllAvailableCottagesTerms(dateFromPar, dateToPar, persons, city, country){
        let headers = {
            'Accept': 'application/json',
            'Content-Type': 'application/json',
            'Authorization': 'Bearer ' + LogInService.accessToken
        };
        let body = {
            dateFrom: dateFromPar.replaceAll('T', ' '),
            dateTo: dateToPar.replaceAll('T', ' '),
            persons: persons,
            city: city,
            country: country,
            id: LogInService.userId
        }
        return axios.post(URL + '/availableCottages/', JSON.stringify(body), {headers});
    }

    reserveCottage(cottageId, selectedServices, from, to){
        const headers = {
            'Accept': 'application/json',
            'Content-Type': 'application/json',
            'Authorization': 'Bearer ' + LogInService.accessToken
        };
        let data = {};
        data.cottageId = cottageId;
        data.userId = LogInService.userId;
        data.from = from;
        data.to = to;
        data.services = selectedServices;

        return axios.put(URL + '/reserveTerm/', JSON.stringify(data), {headers});
    }

    reserveCottageForCustomer(cottageId, selectedServices, from, to, customerId){
        const headers = {
            'Accept': 'application/json',
            'Content-Type': 'application/json',
            'Authorization': 'Bearer ' + LogInService.accessToken
        };
        let data = {};
        data.cottageId = cottageId;
        data.userId = customerId;
        data.from = from;
        data.to = to;
        data.services = selectedServices;

        return axios.put(URL + '/reserveTerm/', JSON.stringify(data), {headers});
    }

    getAllFutureTermsByCustomerId(customerId){
        const headers = {
            'Accept': 'application/json',
            'Content-Type': 'application/json',
            'Authorization': 'Bearer ' + LogInService.accessToken
        };
        return axios.get(URL + '/futureCustomerReservation/' + customerId, {headers});
    }

    //OVO MI NE TREBA
    getAllFastReservations(){
        const headers = {
            'Accept': 'application/json',
            'Content-Type': 'application/json',
            'Authorization': 'Bearer ' + LogInService.accessToken
        };
        return axios.get(URL + '/fastReservations/', {headers});
    }

    getAllFastReservationsOfOwner(id){
        const headers = {
            'Accept': 'application/json',
            'Content-Type': 'application/json',
            'Authorization': 'Bearer ' + LogInService.accessToken
        };
        return axios.get(URL + '/ownerFastReservation/'+id, {headers});
    }

    getAllReservationsOfOwner(id){
        const headers = {
            'Accept': 'application/json',
            'Content-Type': 'application/json',
            'Authorization': 'Bearer ' + LogInService.accessToken
        };
        return axios.get(URL + '/ownerReservation/'+id, {headers});
    }

    cancelReservation(reservation){
        const headers = {
            'Accept': 'application/json',
            'Content-Type': 'application/json',
            'Authorization': 'Bearer ' + LogInService.accessToken
        };
        let data = {};
        data.reservationId = reservation.id;
        data.userId = LogInService.userId;

        return axios.put(URL + '/cancelTerm/', JSON.stringify(data), {headers});
    }

    getAllPastTermsByCustomerId(customerId){
        const headers = {
            'Accept': 'application/json',
            'Content-Type': 'application/json',
            'Authorization': 'Bearer ' + LogInService.accessToken
        };
        return axios.get(URL + '/pastCustomerReservation/' + customerId, {headers});
    }

    AddPeriodForFastReservation(period) {
        const headers = {
            'Accept': 'application/json',
            'Content-Type': 'application/json',
            'Authorization': 'Bearer ' + LogInService.accessToken
        };
        return axios.put(URL + '/fastReservationPeriod/', JSON.stringify(period), {headers});
    }

    getAllPastTermsByCustomerIdWithPutComplaint(customerId){
        const headers = {
            'Accept': 'application/json',
            'Content-Type': 'application/json',
            'Authorization': 'Bearer ' + LogInService.accessToken
        };
        return axios.get(URL + '/pastCustomerReservationWithOutComplaint/' + customerId, {headers});
    }

    getAllNextActionsOfCottage(id) {
        const headers = {
            'Accept': 'application/json',
            'Content-Type': 'application/json',
            'Authorization': 'Bearer ' + LogInService.accessToken
        };
        return axios.get(URL + '/nextActions/' + id, {headers});
    }

    ReserveFastReservation(reservationId){
        const headers = {
            'Accept': 'application/json',
            'Content-Type': 'application/json',
            'Authorization': 'Bearer ' + LogInService.accessToken
        };
        let data = {};
        data.reservationId = reservationId;
        data.userId = LogInService.userId;

        return axios.put(URL + '/reserveFastReservation/', JSON.stringify(data), {headers});
    }
}
export default new CottageReservationService()
