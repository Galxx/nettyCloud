package messages;

public class AuthMsg extends AbstractMsg {
    private String login;
    private String password;
    private String nickname;

    public AuthMsg(String login, String password) {
        this.login = login;
        this.password = password;
    }

    public AuthMsg(String nickname) {
        this.nickname = nickname;
    }


    public String getLogin() {
        return login;
    }

    public String getPassword() {
        return password;
    }

    public String getNickname() {
        return nickname;
    }


}
