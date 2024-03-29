import LogInService from "@/Services/LogInService";
import axios from "axios";

const URL = process.env.VUE_APP_BACKEND_URL + 'report';

class ReportService{

    getAllReportsForProcessing(){
        const headers = {
            'Accept': 'application/json',
            'Content-Type': 'application/json',
            'Authorization': 'Bearer ' + LogInService.accessToken
        };
        return axios.get(URL + '/notProcessed', {headers});
    }

    penalizeCustomer(reportId){
        const headers = {
            'Accept': 'application/json',
            'Content-Type': 'application/json',
            'Authorization': 'Bearer ' + LogInService.accessToken
        };
        return axios.put(URL + '/penalize/' + reportId, {}, {headers});
    }

    haveMercyOnCustomer(reportId){
        const headers = {
            'Accept': 'application/json',
            'Content-Type': 'application/json',
            'Authorization': 'Bearer ' + LogInService.accessToken
        };
        return axios.put(URL + '/mercy/' + reportId, {}, {headers});
    }

    // data je MakeReportDto {
    //     private String comment;
    //     private boolean customerAppear;
    //     private boolean badReport;
    //     private long reservationId;
    //     //AR, BR, CR
    //     private String reservationType;
    makeReport(data){
        const headers = {
            'Accept': 'application/json',
            'Content-Type': 'application/json',
            'Authorization': 'Bearer ' + LogInService.accessToken
        };
        return axios.post(URL, JSON.stringify(data), {headers})
    }
}
export default new ReportService()
