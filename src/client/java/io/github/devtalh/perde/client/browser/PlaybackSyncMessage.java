package io.github.devtalh.perde.client.browser;

/** Fixed, bounded observation protocol. It accepts no page commands or URLs. */
record PlaybackSyncMessage(long poll, long element, double area, PlaybackSyncDecision.Media media, boolean playRejected) {
    static final int MAX_LENGTH = 512;

    static PlaybackSyncMessage parse(String request, String token, long expectedPoll) {
        if (request == null || request.length() > MAX_LENGTH) return null;
        String[] fields = request.split("\\|", -1);
        if (fields.length != 12 || !fields[0].equals("p1") || !fields[1].equals(token)) return null;
        try {
            long poll = Long.parseLong(fields[2]), element = Long.parseLong(fields[3]);
            double area = Double.parseDouble(fields[4]), position = Double.parseDouble(fields[5]);
            double duration = fields[6].equals("live") ? Double.POSITIVE_INFINITY : Double.parseDouble(fields[6]);
            boolean paused = bit(fields[7]), live = bit(fields[9]), rejected = bit(fields[10]);
            int ready = Integer.parseInt(fields[8]);
            if (poll != expectedPoll || element < 0 || !Double.isFinite(area) || area < 0 || area > 1e10
                    || !Double.isFinite(position) || position < 0 || position > 1e15
                    || (!Double.isFinite(duration) && !fields[6].equals("live")) || duration < 0 || duration > 1e15 && Double.isFinite(duration)
                    || ready < 0 || ready > 4 || !fields[11].equals("end")
                    || element == 0 && (area != 0 || position != 0) || element != 0 && area <= 0) return null;
            return new PlaybackSyncMessage(poll, element, area, new PlaybackSyncDecision.Media(position, duration, paused, ready, live), rejected);
        } catch (IllegalArgumentException exception) { return null; }
    }

    private static boolean bit(String value) {
        if (value.equals("1")) return true;
        if (value.equals("0")) return false;
        throw new IllegalArgumentException("Invalid bit");
    }
}
