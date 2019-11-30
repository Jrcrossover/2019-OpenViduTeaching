import { UserHandler, User } from '../users/user.module';
import { Injectable } from "@angular/core";
import { HttpClient } from "@angular/common/http";
import 'rxjs/Rx';
import { Observable } from 'rxjs';
import { map, catchError } from "rxjs/operators";

@Injectable({
  providedIn: 'root'
})

export class RoomService {

  baseURL: string = '/ovTeachingApi';

  constructor(
    private http: HttpClient,
    private userHandler: UserHandler
  ) { }

  createRoom(roomName: string): Observable<string> {
    return this.http.post<string>(this.baseURL + '/room/' + roomName, {}).pipe(
      map(roomName => { return roomName }),
      catchError((error) => this.handleError(error))
    );
  }

  getRoomCode(roomName: string, role: string): Observable<string> {
    return this.http.get(this.baseURL + '/room/' + roomName + '/inviteCode/' + role, { responseType: 'text' }).pipe(
      map(code => { return code }),
      catchError((error) => this.handleError(error))
    );
  }

  checkRoom(codeOrName: string): Observable<string> {
    return this.http.get(this.baseURL + '/room/' + codeOrName, { responseType: 'text' }).pipe(
      map(roomName => { return roomName }),
      catchError((error) => this.handleError(error))
    );
  }

  enterRoom(code: string, userName: string): Observable<User> {
    return this.http.put<User>(this.baseURL + '/room/' + code + '/user/' + userName, {}).pipe(
      map(user => {
        let auth = window.btoa(userName + ':pass'); //TODO change pass
        this.userHandler.saveUser(user, auth);
        return user;
      }),
      catchError((error) => this.handleError(error))
    );
  }

  private handleError(error: any) {
    console.error(error);
    return Observable.throw("Server error (" + error.status + "): " + error.text())
  }
}