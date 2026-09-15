package es.workfactory.occupancy.domain;

public final class Ages {

    private Ages() {}

    /**
     * Age in completed years.
     *
     * It comes solved, and solved over the parts of the string on purpose: with LocalDate
     * and a time zone in the middle, someone born right on the boundary changes age
     * depending on the machine it runs on.
     */
    public static int on(String birthDate, String reference) {
        String[] birth = birthDate.split("-");
        String[] moment = reference.split("-");
        int birthYear = Integer.parseInt(birth[0]);
        int birthMonth = Integer.parseInt(birth[1]);
        int birthDay = Integer.parseInt(birth[2]);
        int year = Integer.parseInt(moment[0]);
        int month = Integer.parseInt(moment[1]);
        int day = Integer.parseInt(moment[2]);
        boolean hasHadBirthday = month > birthMonth || (month == birthMonth && day >= birthDay);
        return year - birthYear - (hasHadBirthday ? 0 : 1);
    }
}
