import axios from "axios";
import LogInService from "@/Services/LogInService";

const URL = process.env.VUE_APP_BACKEND_URL + 'adventure';
const URL_AS = process.env.VUE_APP_BACKEND_URL + 'additionalServices';
const URL_IMG = process.env.VUE_APP_BACKEND_URL + 'images';

class AdventureService{
    adventureId = ''

    getAllAdventures(){
        const headers = {
            'Accept': 'application/json',
            'Content-Type': 'application/json',
            'Authorization': 'Bearer ' + LogInService.accessToken
        };
        return axios.get(URL, {headers});
    }

    getAdventureById(adventureId){
        const headers = {
            'Accept': 'application/json',
            'Content-Type': 'application/json',
            'Authorization': 'Bearer ' + LogInService.accessToken
        };
        return axios.get(URL + '/' + adventureId, {headers});
    }

    getAllAdventuresOfFishingInstructor(instructorId){
        const headers = {
            'Accept': 'application/json',
            'Content-Type': 'application/json',
            'Authorization': 'Bearer ' + LogInService.accessToken
        };
        return axios.get(URL + '/instructor/' + instructorId, {headers});
    }

    saveAdventure(adventure, userId){
        const headers = {
            'Accept': 'application/json',
            'Content-Type': 'application/json',
            'Authorization': 'Bearer ' + LogInService.accessToken
        };
        let data = JSON.stringify(adventure);
        return axios.post(URL + '/' + userId, data, {headers});
    }

    deleteAdventure(adventureId){
        const headers = {
            'Accept': 'application/json',
            'Authorization': 'Bearer ' + LogInService.accessToken
        };
        return axios.delete(URL + '/' + adventureId, {headers});
    }

    updateAdventure(adventure){
        const headers = {
            'Accept': 'application/json',
            'Content-Type': 'application/json',
            'Authorization': 'Bearer ' + LogInService.accessToken
        };
        let data = JSON.stringify(adventure);
        return axios.put(URL, data, {headers});
    }

    addAdditionalServices(additionalService, adventureId){
        const headers = {
            'Accept': 'application/json',
            'Content-Type': 'application/json',
            'Authorization': 'Bearer ' + LogInService.accessToken
        };
        return axios.post(URL_AS + '/' + adventureId, JSON.stringify(additionalService),
            {headers});
    }

    getAdditionalServicesOfReservation(reservationId){
        const headers = {
            'Accept': 'application/json',
            'Content-Type': 'application/json',
            'Authorization': 'Bearer ' + LogInService.accessToken
        };
        return axios.get(URL + '/additionalServices/' + reservationId, {headers});
    }

    getAllImagesOfAdventure(adventureId){
        const headers = {
            'Accept': 'application/json',
            'Content-Type': 'application/json',
            'Authorization': 'Bearer ' + LogInService.accessToken
        };
        return axios.get(URL + '/images/' + adventureId, {headers});
    }

    addImage(image, adventureId){
        const headers = {
            'Accept': 'application/json',
            'Content-Type': 'application/json',
            'Authorization': 'Bearer ' + LogInService.accessToken
        };
        return axios.post(URL_IMG + '/' + adventureId, JSON.stringify(image),
            {headers});
    }

    setAdventureId(id){
        this.adventureId = id;
    }

    getAdventureId(){
        return this.adventureId;
    }

    getAdditionalServices(adventureId){
        const headers = {
            'Accept': 'application/json',
            'Content-Type': 'application/json',
            'Authorization': 'Bearer ' + LogInService.accessToken
        };
        return axios.get(URL_AS + '/' + adventureId, {headers});
    }
}

export default new AdventureService()
